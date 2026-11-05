package com.hamburguesas.service;

import com.hamburguesas.dto.BuzonDto;
import com.hamburguesas.dto.NotificacionDto;
import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.model.Rating;
import com.hamburguesas.model.User;
import com.hamburguesas.repository.BurgerJointRepository;
import com.hamburguesas.repository.RatingRepository;
import com.hamburguesas.repository.ReaccionRepository;
import com.hamburguesas.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * "Tu mes en Burgómetro" en la campana y como pop up (#405). Usa el reloj real: las reseñas
 * se ponen a mitad del último mes terminado de hoy, así que anda cualquier día del año.
 */
@SpringBootTest
@ActiveProfiles("test")
class ResumenEnLaCampanaTest {

    @Autowired private NotificacionesService notificaciones;
    @Autowired private TuMes tuMes;
    @Autowired private UserRepository userRepository;
    @Autowired private RatingRepository ratingRepository;
    @Autowired private ReaccionRepository reaccionRepository;
    @Autowired private BurgerJointRepository burgerJointRepository;

    private User ana;
    private int contador;

    @BeforeEach
    void setUp() {
        ana = userRepository.findByUsername("resumen_ana").orElseGet(() -> userRepository.save(
            User.builder().username("resumen_ana").email("resumen_ana@example.com").emailVerified(true).build()));
        ana.setNotificacionesVistasEl(null);
        ana.setResumenVisto(null);
        ana = userRepository.save(ana);
    }

    @AfterEach
    void limpiar() {
        reaccionRepository.deleteAll();
        ratingRepository.deleteAll();
    }

    /** Una reseña a mitad del mes que se resume hoy (el anterior; si es diciembre, cualquier día del año). */
    private void reseniaDelUltimoMes() {
        YearMonth ultimo = TuMes.mesQueSeResume(LocalDate.now(TuMes.ZONA));
        String id = "resumen-" + System.nanoTime() + "-" + (++contador);
        BurgerJoint local = burgerJointRepository.save(
            BurgerJoint.builder().name(id).address("c").area("Palermo").slug(id).placeId(id).build());
        ratingRepository.save(Rating.builder().user(ana).burgerJoint(local).score(4)
            .createdAt(ZonedDateTime.of(LocalDateTime.of(ultimo.getYear(), ultimo.getMonth(), 15, 12, 0), TuMes.ZONA).toInstant())
            .build());
    }

    private NotificacionDto elResumen(BuzonDto buzon) {
        return buzon.notificaciones().stream()
            .filter(n -> n.tipo() == NotificacionDto.Tipo.RESUMEN).findFirst().orElse(null);
    }

    @Test
    void conUnaReseniaEnElUltimoMesApareceEnLaCampanaComoNuevo() {
        reseniaDelUltimoMes();

        BuzonDto buzon = notificaciones.buzon(ana.getId());

        NotificacionDto aviso = elResumen(buzon);
        assertThat(aviso).isNotNull();
        assertThat(aviso.nueva()).isTrue();
        assertThat(aviso.mes()).isEqualTo(TuMes.mesQueSeResume(LocalDate.now(TuMes.ZONA)).toString());
        assertThat(notificaciones.nuevas(ana.getId()).nuevas()).isEqualTo(1);
    }

    @Test
    void sinReseniasEnElPeriodoNoHayAvisoNiPopUp() {
        assertThat(elResumen(notificaciones.buzon(ana.getId()))).isNull();
        assertThat(notificaciones.nuevas(ana.getId()).nuevas()).isZero();
        assertThat(tuMes.aviso(ana.getId()).hayResumen()).isFalse();
    }

    @Test
    void despuesDeAbrirLaCampanaSigueEnLaListaPeroYaNoEsNuevo() {
        reseniaDelUltimoMes();
        notificaciones.marcarVistas(ana.getId());

        NotificacionDto aviso = elResumen(notificaciones.buzon(ana.getId()));

        assertThat(aviso).isNotNull();
        assertThat(aviso.nueva()).isFalse();
        assertThat(notificaciones.nuevas(ana.getId()).nuevas()).isZero();
    }

    @Test
    void elPopUpSaleUnaSolaVezPeroLaNotificacionSigueEnLaCampana() {
        reseniaDelUltimoMes();
        assertThat(tuMes.aviso(ana.getId()).visto()).isFalse();

        tuMes.marcarVisto(ana.getId());
        tuMes.marcarVisto(ana.getId());   // repetirlo no cambia nada

        var aviso = tuMes.aviso(ana.getId());
        assertThat(aviso.hayResumen()).isTrue();
        assertThat(aviso.visto()).isTrue();
        // Ver el pop up no la saca de la campana: se puede volver a abrir cuando quiera.
        assertThat(elResumen(notificaciones.buzon(ana.getId()))).isNotNull();
    }

    @Test
    void elVistoEsDelServidorYNoRetrocede() {
        reseniaDelUltimoMes();
        tuMes.marcarVisto(ana.getId());
        String visto = userRepository.findById(ana.getId()).orElseThrow().getResumenVisto();

        assertThat(visto).isEqualTo(TuMes.mesQueSeResume(LocalDate.now(TuMes.ZONA)).toString());
        // Un resumen más nuevo (el mes que viene) sí vuelve a salir: ese no está visto.
        assertThat(visto.compareTo(YearMonth.from(LocalDate.now(TuMes.ZONA)).toString())).isLessThan(0);
    }
}
