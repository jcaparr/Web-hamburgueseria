package com.hamburguesas.service;

import com.hamburguesas.dto.AuthResponse;
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

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Los premios del avatar contra una base: validar contra el nivel y recortar al bajar (#313). */
@SpringBootTest
@ActiveProfiles("test")
class PremiosEnLaCuentaTest {

    private static final String LARGO =
        "Muy buena la carne, el pan estaba tostado justo y la salsa me encantó bastante; las papas también salieron crocantes";

    @Autowired private UserRepository userRepository;
    @Autowired private RatingRepository ratingRepository;
    @Autowired private BurgerJointRepository burgerJointRepository;
    @Autowired private RecalculoDeXp recalculo;
    @Autowired private PlatformTransactionManager manager;
    private TransactionTemplate transacciones;
    @Autowired private ProfileService profileService;

    private User yo;

    @BeforeEach
    void setUp() {
        transacciones = new TransactionTemplate(manager);
        ratingRepository.deleteAll();
        yo = userRepository.findByUsername("premiosdelavatar").orElseGet(() -> userRepository.save(
            User.builder().username("premiosdelavatar").email("premios@example.com")
                .emailVerified(true).build()));
        yo.setHamburguesa(null);
        yo = userRepository.save(yo);
        conXp(0);
    }

    @AfterEach
    void tearDown() {
        ratingRepository.deleteAll();
        conXp(0);
    }

    private void conXp(int xp) {
        transacciones.executeWithoutResult(t -> userRepository.ponerXp(yo.getId(), xp));
    }

    private User hoy() {
        return userRepository.findById(yo.getId()).orElseThrow();
    }

    @Test
    void sinNivelParaElBaconRespondeConflicto() {
        conXp(200); // nivel 3: huevo sí, bacon no
        assertThat(profileService.cambiarHamburguesa(yo.getId(), "20131100").hamburguesa()).isEqualTo("20131100");

        assertThatThrownBy(() -> profileService.cambiarHamburguesa(yo.getId(), "20131010"))
            .isInstanceOf(ConflictException.class)
            .hasMessage("Todavía no desbloqueaste el bacon");
        assertThat(hoy().getHamburguesa()).isEqualTo("20131100");
    }

    @Test
    void conElNivelAlcanzaParaTodo() {
        conXp(6500);
        AuthResponse r = profileService.cambiarHamburguesa(yo.getId(), "42234111");
        assertThat(r.hamburguesa()).isEqualTo("42234111");
    }

    @Test
    void alBajarDeNivelSeRecortaLaHamburguesa() {
        conXp(1500); // nivel 10: ya tiene cuarta carne, huevo y bacon
        profileService.cambiarHamburguesa(yo.getId(), "20134110");
        BurgerJoint local = burgerJointRepository.findByPlaceId("premios-1").orElseGet(() ->
            burgerJointRepository.save(BurgerJoint.builder().name("L").address("c").area("Palermo")
                .slug("premios-1").placeId("premios-1").build()));
        // Una sola reseña con comentario = 130 XP: nivel 2.
        ratingRepository.save(Rating.builder().user(yo).burgerJoint(local).score(4).comment(LARGO)
            .createdAt(Instant.parse("2026-09-20T15:00:00Z")).build());

        recalculo.recalcular(yo.getId());

        assertThat(hoy().getXp()).isEqualTo(130);
        // Nivel 2: sin huevo, sin bacon y con tres carnes; sin premios son cinco cifras.
        assertThat(hoy().getHamburguesa()).isEqualTo("20133");
    }

    @Test
    void sinHamburguesaElegidaElRecorteNoHaceNada() {
        conXp(1500);
        recalculo.recalcular(yo.getId());
        assertThat(hoy().getHamburguesa()).isNull();
    }
}
