package com.hamburguesas.service;

import com.hamburguesas.dto.BurgerJointDto;
import com.hamburguesas.dto.RankingItemDto;
import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.model.Rating;
import com.hamburguesas.model.User;
import com.hamburguesas.repository.BurgerJointRepository;
import com.hamburguesas.repository.OrdenDeLocales;
import com.hamburguesas.repository.RatingRepository;
import com.hamburguesas.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Explorar y el ranking en un orden fijo (#207), y en Explorar las tres opciones para
 * elegirlo: más relevantes, mejores valoradas y peores valoradas.
 *
 * Explorar no pedía ningún orden, y Postgres devolvía los locales como estaban guardados
 * en el disco: la lista cambiaba sola y paginarla podía repetir o saltear locales.
 *
 * Todo pasa en un barrio propio, para que los locales de otras clases de test, que
 * comparten la base, no se metan en el medio.
 */
@SpringBootTest
@ActiveProfiles("test")
class OrdenDeExplorarTest {

    private static final String BARRIO = "Barrio del orden";

    @Autowired private BurgerJointService service;
    @Autowired private BurgerJointRepository burgerJointRepository;
    @Autowired private RatingRepository ratingRepository;
    @Autowired private UserRepository userRepository;

    private final List<BurgerJoint> creados = new ArrayList<>();

    @BeforeEach
    void setUp() {
        User ana = alguien("anaorden");
        User beto = alguien("betoorden");
        User caro = alguien("caroorden");

        // Creados a propósito en otro orden que el esperado.
        BurgerJoint mismoNombreUno = local("Mismo nombre");
        BurgerJoint beta = local("Beta");
        BurgerJoint atomo = local("Átomo");
        BurgerJoint alfa = local("Alfa");
        BurgerJoint mismoNombreDos = local("Mismo nombre");
        BurgerJoint zeta = local("Zeta");
        BurgerJoint gamma = local("Gamma");

        resenia(ana, zeta, 5);
        resenia(beto, zeta, 5);
        resenia(ana, alfa, 5);
        resenia(ana, beta, 3);
        resenia(ana, gamma, 2);
        resenia(beto, gamma, 2);
        resenia(caro, gamma, 2);
        assertThat(mismoNombreUno.getId()).isLessThan(mismoNombreDos.getId());
        assertThat(atomo.getId()).isNotNull();
    }

    @AfterEach
    void tearDown() {
        List<Long> ids = creados.stream().map(BurgerJoint::getId).toList();
        ratingRepository.deleteAll(ratingRepository.findAll().stream()
            .filter(r -> ids.contains(r.getBurgerJoint().getId())).toList());
        burgerJointRepository.deleteAll(creados);
    }

    private User alguien(String username) {
        return userRepository.findByUsername(username).orElseGet(() -> userRepository.save(
            User.builder().username(username).email(username + "@example.com")
                .emailVerified(true).build()));
    }

    private BurgerJoint local(String nombre) {
        BurgerJoint guardado = burgerJointRepository.save(BurgerJoint.builder()
            .name(nombre).address("Una calle 123").area(BARRIO).build());
        creados.add(guardado);
        return guardado;
    }

    private void resenia(User de, BurgerJoint local, int nota) {
        ratingRepository.save(Rating.builder()
            .user(de).burgerJoint(local).score(nota).comment("algo").build());
    }

    private List<BurgerJointDto> pagina(OrdenDeLocales orden, int numero, int tamanio) {
        return service.search(null, List.of(BARRIO), orden, null, PageRequest.of(numero, tamanio))
            .getContent();
    }

    private List<String> nombres(OrdenDeLocales orden) {
        return pagina(orden, 0, 20).stream().map(BurgerJointDto::name).toList();
    }

    /**
     * Por omisión, los que más reseñas tienen; a igual cantidad, el mejor puntaje. Sin
     * reseñas, al final y por nombre sin acentos; el id desempata lo que queda.
     */
    @Test
    void masRelevantesVaPorCantidadDeResenias() {
        assertThat(nombres(OrdenDeLocales.RELEVANTES))
            .containsExactly("Gamma", "Zeta", "Alfa", "Beta", "Átomo", "Mismo nombre", "Mismo nombre");

        List<BurgerJointDto> todos = pagina(OrdenDeLocales.RELEVANTES, 0, 20);
        assertThat(todos.get(5).id()).isLessThan(todos.get(6).id());
    }

    @Test
    void mejoresValoradasVaPorPuntaje() {
        assertThat(nombres(OrdenDeLocales.MEJORES))
            .containsExactly("Zeta", "Alfa", "Beta", "Gamma", "Átomo", "Mismo nombre", "Mismo nombre");
    }

    /** Los que nadie probó quedan al final también acá: no son los peores, no se sabe. */
    @Test
    void peoresValoradasVaAlReves() {
        assertThat(nombres(OrdenDeLocales.PEORES))
            .containsExactly("Gamma", "Beta", "Zeta", "Alfa", "Átomo", "Mismo nombre", "Mismo nombre");
    }

    /** De a dos, las páginas juntas son la lista entera, sin repetir ni saltear. */
    @Test
    void paginarNoRepiteNiSalteaNinguno() {
        for (OrdenDeLocales orden : OrdenDeLocales.values()) {
            List<Long> deAPartes = new ArrayList<>();
            for (int numero = 0; numero < 4; numero++) {
                pagina(orden, numero, 2).forEach(local -> deAPartes.add(local.id()));
            }

            assertThat(deAPartes).as("orden %s", orden)
                .doesNotHaveDuplicates()
                .containsExactlyElementsOf(pagina(orden, 0, 20).stream().map(BurgerJointDto::id).toList());
        }
    }

    /** Lo que llega en la dirección: sin importar mayúsculas, y lo que no existe es el de siempre. */
    @Test
    void laOpcionSeLeeDeLaDireccion() {
        assertThat(OrdenDeLocales.de("mejores")).isEqualTo(OrdenDeLocales.MEJORES);
        assertThat(OrdenDeLocales.de(" PEORES ")).isEqualTo(OrdenDeLocales.PEORES);
        assertThat(OrdenDeLocales.de(null)).isEqualTo(OrdenDeLocales.RELEVANTES);
        assertThat(OrdenDeLocales.de("cualquiera")).isEqualTo(OrdenDeLocales.RELEVANTES);
    }

    @Test
    void elRankingDesempataPorCantidadDeResenias() {
        List<RankingItemDto> porPuntaje =
            ratingRepository.rankingByScore(BARRIO, PageRequest.of(0, 10)).getContent();
        List<RankingItemDto> porCantidad =
            ratingRepository.rankingByPopularity(BARRIO, PageRequest.of(0, 10)).getContent();

        assertThat(porPuntaje).extracting(RankingItemDto::name).containsExactly("Zeta", "Alfa", "Beta", "Gamma");
        assertThat(porCantidad).extracting(RankingItemDto::name).containsExactly("Gamma", "Zeta", "Alfa", "Beta");
    }
}
