package com.hamburguesas.service;

import com.hamburguesas.dto.TuMesDto;
import com.hamburguesas.exception.ResourceNotFoundException;
import com.hamburguesas.logros.LogroDeUsuarioRepository;
import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.model.Rating;
import com.hamburguesas.model.User;
import com.hamburguesas.repository.BurgerJointRepository;
import com.hamburguesas.repository.RatingRepository;
import com.hamburguesas.repository.ReaccionRepository;
import com.hamburguesas.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** "Tu mes en Burgómetro": qué mes se resume, el borde del período y lo que cuenta (#308). */
@SpringBootTest
@ActiveProfiles("test")
class TuMesTest {

    @Autowired private RatingRepository ratingRepository;
    @Autowired private ReaccionRepository reaccionRepository;
    @Autowired private LogroDeUsuarioRepository logroRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private BurgerJointRepository burgerJointRepository;

    private int contador;

    @AfterEach
    void limpiar() {
        reaccionRepository.deleteAll();
        ratingRepository.deleteAll();
        logroRepository.deleteAll();
    }

    private TuMes servicioAl(String instante) {
        return new TuMes(ratingRepository, logroRepository, userRepository,
            Clock.fixed(Instant.parse(instante), TuMes.ZONA));
    }

    /** El 2 de diciembre al mediodía: noviembre ya terminó. */
    private TuMes servicio() {
        return servicioAl("2026-12-02T15:00:00Z");
    }

    private User cuenta(String nombre) {
        return userRepository.findByUsername(nombre).orElseGet(() -> userRepository.save(
            User.builder().username(nombre).email(nombre + "@example.com").emailVerified(true).build()));
    }

    private BurgerJoint local(String barrio) {
        String id = "tumes-" + (++contador);
        return burgerJointRepository.findByPlaceId(id).orElseGet(() -> burgerJointRepository.save(
            BurgerJoint.builder().name("Local " + id).address("c").area(barrio).slug(id).placeId(id).build()));
    }

    private void resenia(User quien, String barrio, int nota, String enBuenosAires) {
        Instant cuando = ZonedDateTime.of(LocalDateTime.parse(enBuenosAires), TuMes.ZONA).toInstant();
        ratingRepository.save(Rating.builder().user(quien).burgerJoint(local(barrio)).score(nota)
            .createdAt(cuando).build());
    }

    @Test
    void resumeLasReseniasLosBarriosYLaMejorNotaDelMes() {
        User ana = cuenta("tumes_ana");
        resenia(ana, "Palermo", 3, "2026-11-05T20:00:00");
        resenia(ana, "Palermo", 5, "2026-11-10T20:00:00");
        resenia(ana, "Colegiales", 4, "2026-11-12T20:00:00");
        resenia(ana, "Belgrano", 2, "2026-11-20T20:00:00");
        resenia(ana, "Almagro", 2, "2026-11-21T20:00:00");

        TuMesDto resumen = servicio().resumen(ana.getId(), null);

        assertThat(resumen.mes()).isEqualTo("2026-11");
        assertThat(resumen.resenias()).isEqualTo(5);
        assertThat(resumen.mejor().puntaje()).isEqualTo(5);
        assertThat(resumen.mejor().barrio()).isEqualTo("Palermo");
        // Palermo (2) primero; después empatan en 1 y desempata el nombre. Muestra tres de cuatro.
        assertThat(resumen.barrios()).extracting(TuMesDto.Barrio::nombre).containsExactly("Palermo", "Almagro", "Belgrano");
        assertThat(resumen.barriosEnTotal()).isEqualTo(4);
        assertThat(resumen.anio()).isNull();
    }

    @Test
    void elBordeDelMesEsLaMedianocheDeBuenosAires() {
        User ana = cuenta("tumes_borde");
        // 23:30 del último día y 00:30 del primero: en UTC caen ambas ya en el día siguiente.
        resenia(ana, "Palermo", 4, "2026-10-31T23:30:00");   // octubre
        resenia(ana, "Palermo", 4, "2026-11-01T00:30:00");   // noviembre
        resenia(ana, "Palermo", 4, "2026-11-30T23:30:00");   // noviembre
        resenia(ana, "Palermo", 4, "2026-12-01T00:30:00");   // diciembre

        assertThat(servicio().resumen(ana.getId(), YearMonth.of(2026, 11)).resenias()).isEqualTo(2);
        assertThat(servicio().resumen(ana.getId(), YearMonth.of(2026, 10)).resenias()).isEqualTo(1);
    }

    @Test
    void elMesQueTodaviaCorreNoSeResumeYElAnteriorEsElMismoTodoElMes() {
        User ana = cuenta("tumes_curso");

        assertThatThrownBy(() -> servicio().resumen(ana.getId(), YearMonth.of(2026, 12)))
            .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> servicio().resumen(ana.getId(), YearMonth.of(2027, 1)))
            .isInstanceOf(ResourceNotFoundException.class);
        // Cualquier día de diciembre se resume noviembre; el 1 de enero, diciembre.
        for (int dia = 1; dia <= 31; dia++) {
            assertThat(TuMes.mesQueSeResume(LocalDate.of(2026, 12, dia))).isEqualTo(YearMonth.of(2026, 11));
        }
        assertThat(TuMes.mesQueSeResume(LocalDate.of(2027, 1, 1))).isEqualTo(YearMonth.of(2026, 12));
    }

    @Test
    void elDiaEnQueCambiaDeMesSeUsaLaHoraDeBuenosAires() {
        User ana = cuenta("tumes_hora");
        resenia(ana, "Palermo", 4, "2026-11-15T20:00:00");
        // 1/12 02:30 UTC = 30/11 23:30 en Buenos Aires: todavía es noviembre, no hay resumen.
        TuMes aunEsNoviembre = servicioAl("2026-12-01T02:30:00Z");
        assertThatThrownBy(() -> aunEsNoviembre.resumen(ana.getId(), YearMonth.of(2026, 11)))
            .isInstanceOf(ResourceNotFoundException.class);
        // 1/12 03:30 UTC = 1/12 00:30 en Buenos Aires: ya se puede.
        TuMes yaEsDiciembre = servicioAl("2026-12-01T03:30:00Z");
        assertThat(yaEsDiciembre.resumen(ana.getId(), null).resenias()).isEqualTo(1);
    }

    @Test
    void pedirloDeNuevoDaLoMismoYNoCuentaLasDeOtraPersona() {
        User ana = cuenta("tumes_repite");
        User beto = cuenta("tumes_repite_b");
        resenia(ana, "Palermo", 4, "2026-11-15T20:00:00");
        resenia(beto, "Palermo", 5, "2026-11-15T21:00:00");

        TuMesDto primero = servicio().resumen(ana.getId(), null);
        TuMesDto segundo = servicio().resumen(ana.getId(), null);

        assertThat(segundo).isEqualTo(primero);
        assertThat(primero.resenias()).isEqualTo(1);
        assertThat(primero.mejor().puntaje()).isEqualTo(4);
    }

    @Test
    void sinReseniasEnElMesNoHayResumen() {
        User ana = cuenta("tumes_vacio");
        resenia(ana, "Palermo", 4, "2026-10-15T20:00:00");

        TuMesDto resumen = servicio().resumen(ana.getId(), null);

        assertThat(resumen.resenias()).isZero();
        assertThat(resumen.mejor()).isNull();
        assertThat(resumen.barrios()).isEmpty();
    }

    @Test
    void elDeDiciembreTraeLoDelAnioEntero() {
        User ana = cuenta("tumes_anio");
        resenia(ana, "Palermo", 4, "2026-03-10T20:00:00");
        resenia(ana, "Palermo", 4, "2026-07-10T20:00:00");
        resenia(ana, "Belgrano", 5, "2026-12-10T20:00:00");
        resenia(ana, "Belgrano", 4, "2027-01-01T00:30:00");   // ya es del año siguiente

        TuMesDto resumen = servicioAl("2027-01-02T15:00:00Z").resumen(ana.getId(), null);

        assertThat(resumen.mes()).isEqualTo("2026-12");
        assertThat(resumen.resenias()).isEqualTo(1);
        assertThat(resumen.anio()).isEqualTo(new TuMesDto.Anio(3, 3));
    }
}
