package com.hamburguesas.service;

import com.hamburguesas.dto.BurgerJointDto;
import com.hamburguesas.geo.Ubicacion;
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
 * Explorar ordenado por cercanía (#288): primero las franjas de distancia y, dentro de
 * cada una, la calificación.
 *
 * Todo pasa en un barrio propio, como en OrdenDeExplorarTest, y alrededor de un punto
 * inventado en el mar para que ningún local de otra clase quede cerca.
 */
@SpringBootTest
@ActiveProfiles("test")
class OrdenPorCercaniaTest {

    private static final String BARRIO = "Barrio de la cercanía";
    private static final double LAT = -34.50;
    private static final double LON = -58.30;
    // Un grado de latitud son 111 km: 0,001 son unos 111 metros.
    private static final double METRO_EN_LATITUD = 1 / 111_000.0;

    @Autowired private BurgerJointService service;
    @Autowired private BurgerJointRepository burgerJointRepository;
    @Autowired private RatingRepository ratingRepository;
    @Autowired private UserRepository userRepository;

    private final List<BurgerJoint> creados = new ArrayList<>();

    @BeforeEach
    void setUp() {
        User ana = alguien("anacerca");
        User beto = alguien("betocerca");

        // Creados a propósito en otro orden que el esperado.
        BurgerJoint lejosMuyBueno = local("Lejos muy bueno", 6_000);
        BurgerJoint cercaRegular = local("Cerca regular", 300);
        local("Sin coordenadas", null);
        BurgerJoint cercaBueno = local("Cerca bueno", 700);
        BurgerJoint cercaSinResenias = local("Cerca sin reseñas", 100);
        BurgerJoint medioBueno = local("Medio bueno", 1_500);

        resenia(ana, lejosMuyBueno, 5);
        resenia(ana, cercaRegular, 3);
        resenia(ana, cercaBueno, 5);
        resenia(beto, cercaBueno, 4);
        resenia(ana, medioBueno, 5);
        assertThat(cercaSinResenias.getId()).isNotNull();
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

    private BurgerJoint local(String nombre, Integer metrosAlNorte) {
        BurgerJoint.BurgerJointBuilder local = BurgerJoint.builder()
            .name(nombre).address("Una calle 123").area(BARRIO);
        if (metrosAlNorte != null) {
            local.latitude(LAT + metrosAlNorte * METRO_EN_LATITUD).longitude(LON);
        }
        BurgerJoint guardado = burgerJointRepository.save(local.build());
        creados.add(guardado);
        return guardado;
    }

    private void resenia(User de, BurgerJoint local, int nota) {
        ratingRepository.save(Rating.builder()
            .user(de).burgerJoint(local).score(nota).comment("algo").build());
    }

    private List<String> nombres(OrdenDeLocales orden, Ubicacion ubicacion, int tamanio, int numero) {
        return service.search(null, List.of(BARRIO), orden, ubicacion, null, PageRequest.of(numero, tamanio))
            .getContent().stream().map(BurgerJointDto::name).toList();
    }

    /**
     * Franja 0 (menos de 1 km): mejor puntaje primero y, al final de la franja, el que no
     * tiene reseñas. Después la franja de 1 a 2 km, la lejana y, última, la que no tiene
     * coordenadas. Que "Lejos muy bueno" (5 estrellas) quede detrás de "Cerca regular"
     * (3) es justamente lo que se pide: la cercanía manda.
     */
    @Test
    void primeroLaFranjaDeDistanciaYDentroDeEllaLaCalificacion() {
        assertThat(nombres(OrdenDeLocales.CERCANAS, new Ubicacion(LAT, LON), 20, 0))
            .containsExactly("Cerca bueno", "Cerca regular", "Cerca sin reseñas",
                "Medio bueno", "Lejos muy bueno", "Sin coordenadas");
    }

    /** Parado en el otro extremo, lo que era lejano pasa a ser lo más cercano. */
    @Test
    void sigueAQuienMira() {
        Ubicacion cercaDelLejano = new Ubicacion(LAT + 6_000 * METRO_EN_LATITUD, LON);

        assertThat(nombres(OrdenDeLocales.CERCANAS, cercaDelLejano, 20, 0))
            .startsWith("Lejos muy bueno");
    }

    /** Sin ubicación no hay desde dónde medir: es el orden de por omisión, sin error. */
    @Test
    void sinUbicacionSeOrdenaPorRelevancia() {
        assertThat(nombres(OrdenDeLocales.CERCANAS, null, 20, 0))
            .isEqualTo(nombres(OrdenDeLocales.RELEVANTES, null, 20, 0));
    }

    /** De a dos, las páginas juntas son la lista entera, sin repetir ni saltear. */
    @Test
    void paginarNoRepiteNiSalteaNinguno() {
        Ubicacion aca = new Ubicacion(LAT, LON);
        List<String> deAPartes = new ArrayList<>();
        for (int numero = 0; numero < 3; numero++) {
            deAPartes.addAll(nombres(OrdenDeLocales.CERCANAS, aca, 2, numero));
        }

        assertThat(deAPartes).doesNotHaveDuplicates()
            .containsExactlyElementsOf(nombres(OrdenDeLocales.CERCANAS, aca, 20, 0));
    }

    @Test
    void laUbicacionSoloSeAceptaCompletaYEnRango() {
        assertThat(Ubicacion.de(LAT, LON)).isEqualTo(new Ubicacion(LAT, LON));
        assertThat(Ubicacion.de(LAT, null)).isNull();
        assertThat(Ubicacion.de(null, LON)).isNull();
        assertThat(Ubicacion.de(95.0, LON)).isNull();
        assertThat(Ubicacion.de(LAT, Double.NaN)).isNull();
        assertThat(OrdenDeLocales.de("cercanas")).isEqualTo(OrdenDeLocales.CERCANAS);
    }
}
