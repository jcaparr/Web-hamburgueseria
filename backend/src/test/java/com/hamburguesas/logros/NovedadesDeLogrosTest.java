package com.hamburguesas.logros;

import com.hamburguesas.model.User;
import com.hamburguesas.repository.UserRepository;
import com.hamburguesas.service.NotificacionesService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** El aviso al ganar un logro: qué hay para mostrar, que se marque después y la campana (#317). */
@SpringBootTest
@ActiveProfiles("test")
class NovedadesDeLogrosTest {

    @Autowired private Logros logros;
    @Autowired private LogroDeUsuarioRepository logroRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private NotificacionesService notificaciones;

    private User ana;

    @BeforeEach
    void setUp() {
        logroRepository.deleteAll();
        ana = userRepository.findByUsername("novedades_ana").orElseGet(() -> userRepository.save(
            User.builder().username("novedades_ana").email("novedades_ana@example.com").emailVerified(true).build()));
    }

    @AfterEach
    void limpiar() {
        logroRepository.deleteAll();
    }

    @Test
    void loGanadoSinVerSeAvisaYAlMarcarloYaNo() {
        logros.recalcular(ana.getId(), false);

        NovedadesDeLogros n = logros.novedades(ana.getId());
        assertThat(n.sinVer()).contains("NIVEL_1");
        assertThat(n.retroactivos()).isZero();

        logros.marcarVistos(ana.getId(), new LogrosVistos(n.sinVer(), false));

        assertThat(logros.novedades(ana.getId()).sinVer()).isEmpty();
    }

    @Test
    void perderYVolverAGanarNoVuelveAAvisar() {
        logros.recalcular(ana.getId(), false);
        logros.marcarVistos(ana.getId(), new LogrosVistos(logros.novedades(ana.getId()).sinVer(), false));

        // Se desactiva a mano y se vuelve a recalcular: reactiva sin tocar visto_el.
        LogroDeUsuario fila = logroRepository.findById(new LogroDeUsuarioId(ana.getId(), "NIVEL_1")).orElseThrow();
        fila.setActivo(false);
        logroRepository.save(fila);
        logros.recalcular(ana.getId(), false);

        assertThat(logros.novedades(ana.getId()).sinVer()).isEmpty();
    }

    @Test
    void losRetroactivosVanAparteYSeMarcanConSuBandera() {
        logros.recalcular(ana.getId(), true);

        NovedadesDeLogros n = logros.novedades(ana.getId());
        assertThat(n.sinVer()).isEmpty();
        assertThat(n.retroactivos()).isGreaterThanOrEqualTo(1);

        // Marcar los códigos no alcanza: hay que decir que se mostró el aviso único.
        logros.marcarVistos(ana.getId(), new LogrosVistos(List.of("NIVEL_1"), false));
        assertThat(logros.novedades(ana.getId()).retroactivos()).isGreaterThanOrEqualTo(1);

        logros.marcarVistos(ana.getId(), new LogrosVistos(List.of(), true));
        assertThat(logros.novedades(ana.getId()).retroactivos()).isZero();
    }

    @Test
    void marcarNoTocaLosDeOtraCuenta() {
        User beto = userRepository.findByUsername("novedades_beto").orElseGet(() -> userRepository.save(
            User.builder().username("novedades_beto").email("novedades_beto@example.com").emailVerified(true).build()));
        logros.recalcular(ana.getId(), false);
        logros.recalcular(beto.getId(), false);

        logros.marcarVistos(ana.getId(), new LogrosVistos(List.of("NIVEL_1"), false));

        assertThat(logros.novedades(beto.getId()).sinVer()).contains("NIVEL_1");
    }

    @Test
    void laCampanaTraeCuantosLogrosFaltanAvisar() {
        logros.recalcular(ana.getId(), false);
        long sinVer = logros.novedades(ana.getId()).sinVer().size();

        assertThat(notificaciones.nuevas(ana.getId()).logrosSinVer()).isEqualTo(sinVer);

        logros.marcarVistos(ana.getId(), new LogrosVistos(logros.novedades(ana.getId()).sinVer(), false));
        assertThat(notificaciones.nuevas(ana.getId()).logrosSinVer()).isZero();
    }

    @Test
    void losRetroactivosCuentanComoUnSoloAvisoEnLaCampana() {
        logros.recalcular(ana.getId(), true);

        assertThat(notificaciones.nuevas(ana.getId()).logrosSinVer()).isEqualTo(1);
    }
}
