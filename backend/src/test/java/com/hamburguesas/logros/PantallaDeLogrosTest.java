package com.hamburguesas.logros;

import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.model.Rating;
import com.hamburguesas.model.User;
import com.hamburguesas.repository.BurgerJointRepository;
import com.hamburguesas.repository.RatingRepository;
import com.hamburguesas.repository.ReaccionRepository;
import com.hamburguesas.repository.FollowRepository;
import com.hamburguesas.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/** Lo que devuelve GET /api/logros: nivel, XP y cada logro con su avance (#316). */
@SpringBootTest
@ActiveProfiles("test")
class PantallaDeLogrosTest {

    private static final String LARGO =
        "Muy buena la carne, el pan estaba tostado justo y la salsa me encantó bastante; las papas también salieron crocantes";

    @Autowired private Logros logros;
    @Autowired private LogroDeUsuarioRepository logroRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private BurgerJointRepository burgerJointRepository;
    @Autowired private RatingRepository ratingRepository;
    @Autowired private ReaccionRepository reaccionRepository;
    @Autowired private FollowRepository followRepository;

    private User ana;

    @BeforeEach
    void setUp() {
        limpiar();
        ana = userRepository.findByUsername("pantalla_ana").orElseGet(() -> userRepository.save(
            User.builder().username("pantalla_ana").email("pantalla_ana@example.com").emailVerified(true).build()));
    }

    @AfterEach
    void limpiar() {
        reaccionRepository.deleteAll();
        followRepository.deleteAll();
        ratingRepository.deleteAll();
        logroRepository.deleteAll();
    }

    private PantallaDeLogros.Item item(PantallaDeLogros p, Logro logro) {
        return p.logros().stream().filter(i -> i.codigo().equals(logro.name())).findFirst().orElseThrow();
    }

    @Test
    void unaCuentaNuevaTraeTodoElCatalogoYSoloElSliderGanado() {
        PantallaDeLogros p = logros.pantalla(ana.getId());

        assertThat(p.logros()).hasSize(Logro.values().length);
        assertThat(p.nivel()).isEqualTo(1);
        assertThat(p.xp()).isZero();
        assertThat(p.xpDelNivel()).isZero();
        assertThat(p.xpDelSiguiente()).isEqualTo(100);
        assertThat(item(p, Logro.NIVEL_1).ganado()).isTrue();
        assertThat(item(p, Logro.NIVEL_1).fecha()).isNotNull();
        assertThat(item(p, Logro.PRIMERA_RESENIA).ganado()).isFalse();
        assertThat(item(p, Logro.PRIMERA_RESENIA).fecha()).isNull();
    }

    @Test
    void traeElAvanceDeLosQueSeMidenConUnNumero() {
        PantallaDeLogros p = logros.pantalla(ana.getId());

        assertThat(item(p, Logro.NIVEL_3).actual()).isEqualTo(1);
        assertThat(item(p, Logro.NIVEL_3).meta()).isEqualTo(3);
        assertThat(item(p, Logro.SEGUIDORES_10).actual()).isZero();
        assertThat(item(p, Logro.SEGUIDORES_10).meta()).isEqualTo(10);
        // De los que se tienen o no, no hay avance.
        assertThat(item(p, Logro.PRIMERA_RESENIA).meta()).isNull();
    }

    @Test
    void deUnSecretoSinGanarNoSeDiceNada() {
        PantallaDeLogros p = logros.pantalla(ana.getId());

        assertThat(item(p, Logro.COMEDIANTE).ganado()).isFalse();
        assertThat(item(p, Logro.COMEDIANTE).meta()).isNull();
        assertThat(item(p, Logro.COMEDIANTE).actual()).isNull();
    }

    @Test
    void recalculaAntesDeResponder() {
        BurgerJoint local = burgerJointRepository.findByPlaceId("pantalla-1").orElseGet(() ->
            burgerJointRepository.save(BurgerJoint.builder().name("L").address("c").area("Palermo")
                .slug("pantalla-1").placeId("pantalla-1").build()));
        ratingRepository.save(Rating.builder().user(ana).burgerJoint(local).score(4).comment(LARGO)
            .createdAt(Instant.parse("2026-10-20T15:00:00Z")).build());

        PantallaDeLogros p = logros.pantalla(ana.getId());

        // 100 de la reseña con comentario + 30 de bienvenida: nivel 2.
        assertThat(p.xp()).isEqualTo(130);
        assertThat(p.nivel()).isEqualTo(2);
        assertThat(p.xpDelNivel()).isEqualTo(100);
        assertThat(p.xpDelSiguiente()).isEqualTo(200);
        assertThat(item(p, Logro.PRIMERA_RESENIA).ganado()).isTrue();
    }
}
