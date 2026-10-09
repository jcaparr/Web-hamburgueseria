package com.hamburguesas.service;

import com.hamburguesas.dto.BurgerJointDto;
import com.hamburguesas.dto.RankingItemDto;
import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.model.Rating;
import com.hamburguesas.model.User;
import com.hamburguesas.repository.BurgerJointRepository;
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
 * Explorar y el ranking en un orden fijo (#207).
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

        // Creados a propósito en otro orden que el esperado.
        BurgerJoint mismoNombreUno = local("Mismo nombre");
        BurgerJoint beta = local("Beta");
        BurgerJoint atomo = local("Átomo");
        BurgerJoint alfa = local("Alfa");
        BurgerJoint mismoNombreDos = local("Mismo nombre");
        BurgerJoint zeta = local("Zeta");

        resenia(ana, zeta, 5);
        resenia(beto, zeta, 5);
        resenia(ana, alfa, 5);
        resenia(ana, beta, 3);
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

    private List<BurgerJointDto> pagina(int numero, int tamanio) {
        return service.search(null, List.of(BARRIO), true, null, PageRequest.of(numero, tamanio))
            .getContent();
    }

    /**
     * Mejor puntaje primero; a igual puntaje, más reseñas; sin reseñas, al final y por
     * nombre sin acentos; y el id desempata lo que queda.
     */
    @Test
    void explorarVaPorPuntajeReseniasYNombre() {
        List<BurgerJointDto> todos = pagina(0, 20);

        assertThat(todos).extracting(BurgerJointDto::name)
            .containsExactly("Zeta", "Alfa", "Beta", "Átomo", "Mismo nombre", "Mismo nombre");
        assertThat(todos.get(4).id()).isLessThan(todos.get(5).id());
    }

    /** De a dos, las tres páginas juntas son la lista entera, sin repetir ni saltear. */
    @Test
    void paginarNoRepiteNiSalteaNinguno() {
        List<Long> deAPartes = new ArrayList<>();
        for (int numero = 0; numero < 3; numero++) {
            pagina(numero, 2).forEach(local -> deAPartes.add(local.id()));
        }

        assertThat(deAPartes)
            .doesNotHaveDuplicates()
            .containsExactlyElementsOf(pagina(0, 20).stream().map(BurgerJointDto::id).toList());
    }

    @Test
    void elRankingDesempataPorCantidadDeResenias() {
        List<RankingItemDto> porPuntaje =
            ratingRepository.rankingByScore(BARRIO, PageRequest.of(0, 10)).getContent();
        List<RankingItemDto> porCantidad =
            ratingRepository.rankingByPopularity(BARRIO, PageRequest.of(0, 10)).getContent();

        assertThat(porPuntaje).extracting(RankingItemDto::name).containsExactly("Zeta", "Alfa", "Beta");
        assertThat(porCantidad).extracting(RankingItemDto::name).containsExactly("Zeta", "Alfa", "Beta");
    }
}
