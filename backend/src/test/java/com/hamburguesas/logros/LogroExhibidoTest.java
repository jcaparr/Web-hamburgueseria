package com.hamburguesas.logros;

import com.hamburguesas.exception.ConflictException;
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
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** El logro que cada uno muestra en su perfil: qué se puede elegir, cuándo se limpia y qué se oculta (#319). */
@SpringBootTest
@ActiveProfiles("test")
class LogroExhibidoTest {

    @Autowired private Logros logros;
    @Autowired private LogroDeUsuarioRepository logroRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private BurgerJointRepository burgerJointRepository;
    @Autowired private RatingRepository ratingRepository;

    private User ana;

    @BeforeEach
    void setUp() {
        limpiar();
        ana = userRepository.findByUsername("exhibido_ana").orElseGet(() -> userRepository.save(
            User.builder().username("exhibido_ana").email("exhibido_ana@example.com").emailVerified(true).build()));
        ana.setLogroExhibido(null);
        ana.setMuestraListas(true);
        ana = userRepository.save(ana);
    }

    @AfterEach
    void limpiar() {
        ratingRepository.deleteAll();
        logroRepository.deleteAll();
    }

    @Test
    void sePuedeExhibirUnoGanadoYSacarloConNull() {
        logros.recalcular(ana.getId(), false);

        assertThat(logros.exhibir(ana.getId(), "NIVEL_1").getLogroExhibido()).isEqualTo("NIVEL_1");
        assertThat(logros.exhibir(ana.getId(), null).getLogroExhibido()).isNull();
    }

    @Test
    void noSePuedeExhibirUnoNoGanadoNiInventado() {
        logros.recalcular(ana.getId(), false);

        assertThatThrownBy(() -> logros.exhibir(ana.getId(), "COMBO")).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> logros.exhibir(ana.getId(), "NO_EXISTE")).isInstanceOf(ConflictException.class);
    }

    @Test
    void alPerderElLogroSeLimpiaDelPerfil() {
        BurgerJoint local = burgerJointRepository.findByPlaceId("exh-1").orElseGet(() ->
            burgerJointRepository.save(BurgerJoint.builder().name("L").address("c").area("Palermo")
                .slug("exh-1").placeId("exh-1").build()));
        Rating r = ratingRepository.save(Rating.builder().user(ana).burgerJoint(local).score(4).comment("ok")
            .createdAt(Instant.now()).build());
        logros.recalcular(ana.getId(), false);
        logros.exhibir(ana.getId(), "PRIMERA_RESENIA");

        ratingRepository.delete(r);
        logros.recalcular(ana.getId(), false);

        assertThat(userRepository.findById(ana.getId()).orElseThrow().getLogroExhibido()).isNull();
    }

    @Test
    void enUnPerfilAjenoNoSeMuestraElSliderNiLoQueDependeDeLasListasOcultas() {
        logros.recalcular(ana.getId(), false);
        // Se fuerza uno que depende de las listas.
        logroRepository.save(new LogroDeUsuario(new LogroDeUsuarioId(ana.getId(), "PLANIFICADOR"),
            Instant.now(), true, Instant.now(), null, false, null));
        ana.setMuestraListas(false);
        userRepository.save(ana);
        logros.exhibir(ana.getId(), "PLANIFICADOR");

        PerfilDeLogros paraOtro = logros.paraPerfil(ana.getId(), -99L);
        assertThat(paraOtro.activos()).doesNotContain("NIVEL_1", "PLANIFICADOR");
        assertThat(paraOtro.exhibido()).isNull();

        PerfilDeLogros paraMi = logros.paraPerfil(ana.getId(), ana.getId());
        assertThat(paraMi.activos()).contains("NIVEL_1", "PLANIFICADOR");
        assertThat(paraMi.exhibido()).isEqualTo("PLANIFICADOR");
    }
}
