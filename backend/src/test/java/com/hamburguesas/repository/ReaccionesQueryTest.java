package com.hamburguesas.repository;

import com.hamburguesas.dto.CuantasReaccionesDto;
import com.hamburguesas.dto.ReaccionesDto;
import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.model.Rating;
import com.hamburguesas.model.Reaccion;
import com.hamburguesas.model.TipoDeReaccion;
import com.hamburguesas.model.User;
import com.hamburguesas.service.Reacciones;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Las consultas de las reacciones, ejecutadas de verdad contra una base (#186).
 *
 * Los demás tests mockean el repositorio, así que el SQL nunca corre en ellos: esto es
 * lo que atrapa que la consulta deje de armarse o que el borrado en cascada no esté.
 */
@SpringBootTest
@ActiveProfiles("test")
class ReaccionesQueryTest {

    @Autowired private ReaccionRepository reaccionRepository;
    @Autowired private RatingRepository ratingRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private BurgerJointRepository burgerJointRepository;
    @Autowired private Reacciones reacciones;

    private User autor;
    private User una;
    private User otra;
    private User tercera;
    private BurgerJoint local;

    /** Lo mismo que en FeedQueryTest: se buscan antes de crearlos, porque la base es compartida. */
    @BeforeEach
    void setUp() {
        reaccionRepository.deleteAll();
        ratingRepository.deleteAll();
        autor = alguien("autorreac");
        una = alguien("unareac");
        otra = alguien("otrareac");
        tercera = alguien("tercerareac");
        local = burgerJointRepository.findByPlaceId("place-del-test-de-reacciones")
            .orElseGet(() -> burgerJointRepository.save(BurgerJoint.builder()
                .name("Un local").address("Una calle 123").placeId("place-del-test-de-reacciones").build()));
    }

    @AfterEach
    void tearDown() {
        reaccionRepository.deleteAll();
        ratingRepository.deleteAll();
    }

    private User alguien(String username) {
        return userRepository.findByUsername(username).orElseGet(() -> userRepository.save(
            User.builder().username(username).email(username + "@example.com").emailVerified(true).build()));
    }

    private Rating resenia(User de) {
        return ratingRepository.save(Rating.builder().user(de).burgerJoint(local).score(4).build());
    }

    private void reacciona(User quien, Rating a, TipoDeReaccion tipo) {
        reaccionRepository.save(Reaccion.builder().user(quien).rating(a).tipo(tipo).build());
    }

    @Test
    void cuentaCadaTipoYMarcaLaDeQuienMira() {
        Rating r = resenia(autor);
        reacciona(una, r, TipoDeReaccion.FUEGO);
        reacciona(otra, r, TipoDeReaccion.FUEGO);
        reacciona(tercera, r, TipoDeReaccion.HAMBRE);

        ReaccionesDto vistas = reacciones.de(List.of(r.getId()), tercera.getId()).get(r.getId());

        assertThat(vistas.cuantas()).containsExactly(
            new CuantasReaccionesDto(TipoDeReaccion.FUEGO, 2),
            new CuantasReaccionesDto(TipoDeReaccion.HAMBRE, 1));
        assertThat(vistas.mia()).isEqualTo(TipoDeReaccion.HAMBRE);
    }

    /** Sin sesión se ven las cantidades, y ninguna es de quien mira. */
    @Test
    void sinSesionNingunaEsMia() {
        Rating r = resenia(autor);
        reacciona(una, r, TipoDeReaccion.RISA);

        assertThat(reacciones.de(List.of(r.getId()), null).get(r.getId()).mia()).isNull();
    }

    @Test
    void unaReseniaSinReaccionesVieneVacia() {
        Rating r = resenia(autor);

        assertThat(reacciones.de(List.of(r.getId()), una.getId()).get(r.getId()))
            .isEqualTo(ReaccionesDto.NINGUNA);
    }

    /** Borrar la reseña se lleva sus reacciones: no quedan apuntando a algo que ya no está. */
    @Test
    void borrarLaReseniaBorraSusReacciones() {
        Rating r = resenia(autor);
        reacciona(una, r, TipoDeReaccion.APLAUSO);

        ratingRepository.delete(ratingRepository.findById(r.getId()).orElseThrow());
        ratingRepository.flush();

        assertThat(reaccionRepository.count()).isZero();
    }
}
