package com.hamburguesas.service;

import com.hamburguesas.dto.BuzonDto;
import com.hamburguesas.dto.NotificacionDto;
import com.hamburguesas.logros.LogroDeUsuario;
import com.hamburguesas.logros.LogroDeUsuarioId;
import com.hamburguesas.logros.LogroDeUsuarioRepository;
import com.hamburguesas.logros.Logros;
import com.hamburguesas.model.User;
import com.hamburguesas.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/** Los logros como tercera fuente de la campana (#320). */
@SpringBootTest
@ActiveProfiles("test")
class LogrosEnLaCampanaTest {

    @Autowired private NotificacionesService notificaciones;
    @Autowired private Logros logros;
    @Autowired private LogroDeUsuarioRepository logroRepository;
    @Autowired private UserRepository userRepository;

    private User ana;

    @BeforeEach
    void setUp() {
        logroRepository.deleteAll();
        ana = userRepository.findByUsername("campana_ana").orElseGet(() -> userRepository.save(
            User.builder().username("campana_ana").email("campana_ana@example.com").emailVerified(true).build()));
        ana.setNotificacionesVistasEl(null);
        ana = userRepository.save(ana);
    }

    @AfterEach
    void limpiar() {
        logroRepository.deleteAll();
    }

    private LogroDeUsuario fila(String codigo, Instant cuando, boolean retroactivo) {
        return logroRepository.save(new LogroDeUsuario(new LogroDeUsuarioId(ana.getId(), codigo),
            cuando, true, cuando, null, retroactivo, null));
    }

    @Test
    void unLogroGanadoApareceNuevoYCuentaEnLaCampana() {
        fila("SEGUIDORES_10", Instant.now().minusSeconds(60), false);

        BuzonDto buzon = notificaciones.buzon(ana.getId());

        assertThat(buzon.notificaciones()).singleElement().satisfies(n -> {
            assertThat(n.tipo()).isEqualTo(NotificacionDto.Tipo.LOGRO);
            assertThat(n.logro()).isEqualTo("SEGUIDORES_10");
            assertThat(n.nueva()).isTrue();
        });
        assertThat(notificaciones.nuevas(ana.getId()).nuevas()).isEqualTo(1);
    }

    @Test
    void siSeDesactivaDesapareceDeLaCampana() {
        LogroDeUsuario f = fila("INTENDENTE", Instant.now().minusSeconds(60), false);
        f.setActivo(false);
        logroRepository.save(f);

        assertThat(notificaciones.buzon(ana.getId()).notificaciones()).isEmpty();
    }

    @Test
    void unLogroVencidoNoSeAvisa() {
        fila("SEGUIDORES_50", Instant.now().minus(Duration.ofDays(8)), false);

        assertThat(notificaciones.buzon(ana.getId()).notificaciones()).isEmpty();
    }

    @Test
    void losRetroactivosVanEnUnSoloAvisoQueNoLlenaLaCampana() {
        fila("NIVEL_1", Instant.now().minusSeconds(120), true);
        fila("PRIMERA_RESENIA", Instant.now().minusSeconds(120), true);
        fila("NIVEL_3", Instant.now().minusSeconds(120), true);

        BuzonDto buzon = notificaciones.buzon(ana.getId());

        assertThat(buzon.notificaciones()).singleElement().satisfies(n -> {
            assertThat(n.logro()).isEqualTo("RETROACTIVOS");
            assertThat(n.cantidad()).isEqualTo(3);
            assertThat(n.nueva()).isFalse();
        });
        assertThat(buzon.nuevas()).isZero();
        assertThat(notificaciones.nuevas(ana.getId()).nuevas()).isZero();
    }

    @Test
    void lasCifrasDelMotorSeVenEnLaCampanaDespuesDeRecalcular() {
        logros.recalcular(ana.getId(), false);

        assertThat(notificaciones.buzon(ana.getId()).notificaciones())
            .extracting(NotificacionDto::logro).contains("NIVEL_1");
    }
}
