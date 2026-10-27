package com.hamburguesas.repository;

import com.hamburguesas.dto.DesgloseDeNotasDto;
import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.model.Rating;
import com.hamburguesas.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** El desglose de notas contra la base (#336): cada nota cuenta sus propias reseñas. */
@SpringBootTest
@ActiveProfiles("test")
class DesgloseDeNotasEnLaBaseTest {

    @Autowired private RatingRepository ratingRepository;
    @Autowired private BurgerJointRepository burgerJointRepository;
    @Autowired private UserRepository userRepository;

    @BeforeEach
    @AfterEach
    void limpiar() {
        ratingRepository.deleteAll();
    }

    private User cuenta(String nombre) {
        return userRepository.findByUsername(nombre).orElseGet(() -> userRepository.save(
            User.builder().username(nombre).email(nombre + "@example.com").emailVerified(true).build()));
    }

    @Test
    void promediaCadaNotaSobreLasReseniasQueLaPusieron() {
        BurgerJoint local = burgerJointRepository.findByPlaceId("desg-1").orElseGet(() ->
            burgerJointRepository.save(BurgerJoint.builder().name("Desglose").address("Calle 1")
                .slug("desglose-1").placeId("desg-1").build()));
        ratingRepository.save(Rating.builder().user(cuenta("desg_a")).burgerJoint(local).score(5)
            .scoreLocal(5).scoreAtencion(4).visitDate(LocalDate.now().minusDays(2)).build());
        ratingRepository.save(Rating.builder().user(cuenta("desg_b")).burgerJoint(local).score(1)
            .scoreLocal(3).build());
        ratingRepository.save(Rating.builder().user(cuenta("desg_c")).burgerJoint(local).score(3).build());

        DesgloseDeNotasDto d = ratingRepository.desgloseDeNotas(local.getId());

        assertThat(d.local()).isCloseTo(4.0, within(0.001));
        assertThat(d.cuantasLocal()).isEqualTo(2);
        assertThat(d.atencion()).isCloseTo(4.0, within(0.001));
        assertThat(d.cuantasAtencion()).isEqualTo(1);
        assertThat(d.precio()).isNull();
        assertThat(d.cuantasPrecio()).isZero();
        // El promedio que cuenta sigue saliendo solo de la nota general: (5 + 1 + 3) / 3.
        Object[] fila = ratingRepository.promediosPorLocal().stream()
            .filter(f -> f[0].equals(local.getId())).findFirst().orElseThrow();
        assertThat((Double) fila[1]).isCloseTo(3.0, within(0.001));
    }
}
