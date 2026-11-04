package com.hamburguesas.service;

import com.hamburguesas.dto.TuMesDto;
import com.hamburguesas.exception.ResourceNotFoundException;
import com.hamburguesas.logros.LogroDeUsuario;
import com.hamburguesas.logros.LogroDeUsuarioId;
import com.hamburguesas.logros.LogroDeUsuarioRepository;
import com.hamburguesas.podio.ResultadoDePodio;
import com.hamburguesas.podio.ResultadoDePodioRepository;
import com.hamburguesas.model.Block;
import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.model.Rating;
import com.hamburguesas.model.Reaccion;
import com.hamburguesas.model.TipoDeReaccion;
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
    @Autowired private ResultadoDePodioRepository podioRepository;
    @Autowired private com.hamburguesas.service.Bloqueos bloqueos;
    @Autowired private com.hamburguesas.repository.BlockRepository blockRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private BurgerJointRepository burgerJointRepository;

    private int contador;

    @AfterEach
    void limpiar() {
        blockRepository.deleteAll();
        reaccionRepository.deleteAll();
        ratingRepository.deleteAll();
        logroRepository.deleteAll();
        podioRepository.deleteAll();
    }

    private TuMes servicioAl(String instante) {
        return new TuMes(ratingRepository, logroRepository, podioRepository, reaccionRepository, bloqueos, userRepository,
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
        // Único por prueba: los locales quedan en la base y otro test podía haberlo creado con otro barrio.
        String id = "tumes-" + System.nanoTime() + "-" + (++contador);
        return burgerJointRepository.findByPlaceId(id).orElseGet(() -> burgerJointRepository.save(
            BurgerJoint.builder().name("Local " + id).address("c").area(barrio).slug(id).placeId(id).build()));
    }

    private Rating resenia(User quien, String barrio, int nota, String enBuenosAires) {
        Instant cuando = ZonedDateTime.of(LocalDateTime.parse(enBuenosAires), TuMes.ZONA).toInstant();
        return ratingRepository.save(Rating.builder().user(quien).burgerJoint(local(barrio)).score(nota)
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
        // Palermo (2) primero; después empatan en 1 y desempata el nombre. Vienen todos: la tarjeta recorta, la lista completa no.
        assertThat(resumen.barrios()).extracting(TuMesDto.Barrio::nombre).containsExactly("Palermo", "Almagro", "Belgrano", "Colegiales");
        assertThat(resumen.barriosEnTotal()).isEqualTo(4);
        assertThat(resumen.anual()).isFalse();
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

@Test    void unMesAbsurdoDaNoEncontradoYNoUnErrorDeFecha() {        User ana = cuenta("tumes_absurdo");        assertThatThrownBy(() -> servicio().resumen(ana.getId(), YearMonth.of(-999999999, 1)))            .isInstanceOf(ResourceNotFoundException.class);    }
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
    void elResumenDeDiciembreEsElRecuentoDelAnioEntero() {
        User ana = cuenta("tumes_anio");
        resenia(ana, "Palermo", 4, "2025-06-10T20:00:00");      // el año anterior
        resenia(ana, "Palermo", 4, "2026-01-01T00:30:00");      // primer borde del año
        resenia(ana, "Palermo", 5, "2026-03-10T20:00:00");
        resenia(ana, "Belgrano", 3, "2026-03-20T20:00:00");
        resenia(ana, "Belgrano", 4, "2026-07-10T20:00:00");
        resenia(ana, "Almagro", 2, "2026-12-31T23:30:00");      // último borde del año
        resenia(ana, "Almagro", 4, "2027-01-01T00:30:00");      // ya es del año siguiente

        TuMesDto resumen = servicioAl("2027-01-02T15:00:00Z").resumen(ana.getId(), null);

        assertThat(resumen.mes()).isEqualTo("2026-12");
        assertThat(resumen.anual()).isTrue();
        assertThat(resumen.resenias()).isEqualTo(5);
        assertThat(resumen.reseniasAnteriores()).isEqualTo(1);
        assertThat(resumen.mejor().puntaje()).isEqualTo(5);
        assertThat(resumen.barriosEnTotal()).isEqualTo(3);
        // Marzo tiene dos, y es el mes con más reseñas.
        assertThat(resumen.recuento().mesMasActivo()).isEqualTo(3);
        assertThat(resumen.recuento().reseniasDelMes()).isEqualTo(2);
        // Empezó el año con una reseña (30 + 70 = 100 XP: nivel 2) y lo terminó con seis (30 + 6 x 70 = 450 XP: nivel 4).
        assertThat(resumen.recuento().nivelAlEmpezar()).isEqualTo(2);
        assertThat(resumen.recuento().nivelAlTerminar()).isEqualTo(4);
    }

    @Test
    void unMesComunNoTraeElRecuento() {
        User ana = cuenta("tumes_nomes");
        resenia(ana, "Palermo", 4, "2026-11-15T20:00:00");

        TuMesDto resumen = servicio().resumen(ana.getId(), YearMonth.of(2026, 11));

        assertThat(resumen.anual()).isFalse();
        assertThat(resumen.recuento()).isNull();
    }

    @Test
    void comparaConElMesAnteriorYConCeroSiNoReseno() {
        User ana = cuenta("tumes_compara");
        resenia(ana, "Palermo", 4, "2026-10-31T23:30:00");   // octubre
        resenia(ana, "Palermo", 4, "2026-10-05T20:00:00");
        resenia(ana, "Palermo", 4, "2026-11-01T00:30:00");   // noviembre
        resenia(ana, "Palermo", 4, "2026-11-15T20:00:00");
        resenia(ana, "Palermo", 4, "2026-11-20T20:00:00");
        User beto = cuenta("tumes_compara_b");
        resenia(beto, "Palermo", 4, "2026-11-15T20:00:00");

        assertThat(servicio().resumen(ana.getId(), YearMonth.of(2026, 11)).reseniasAnteriores()).isEqualTo(2);
        // Beto no reseñó en octubre: el anterior es 0 y la pantalla lo dice.
        assertThat(servicio().resumen(beto.getId(), YearMonth.of(2026, 11)).reseniasAnteriores()).isZero();
    }

    @Test
    void elDiaFavoritoEsElQueMasReseniaConLaFranjaDeEsoDiaEnHoraDeBuenosAires() {
        User ana = cuenta("tumes_dia");
        // Jueves 5, 12 y 19 de noviembre de 2026, de noche; y un lunes de mañana.
        resenia(ana, "Palermo", 4, "2026-11-05T21:00:00");
        resenia(ana, "Palermo", 4, "2026-11-12T22:30:00");
        resenia(ana, "Palermo", 4, "2026-11-19T20:15:00");
        resenia(ana, "Palermo", 4, "2026-11-09T09:00:00");

        TuMesDto.Dia dia = servicio().resumen(ana.getId(), YearMonth.of(2026, 11)).dia();

        assertThat(dia).isEqualTo(new TuMesDto.Dia(4, "noche", 3));
    }

    @Test
    void elDiaFavoritoRespetaLaMedianocheDeBuenosAiresYElEmpateGanaElDiaMasTemprano() {
        User ana = cuenta("tumes_dia_borde");
        // Domingo 23:30 en Buenos Aires ya es lunes en UTC: tiene que contar como domingo.
        resenia(ana, "Palermo", 4, "2026-11-08T23:30:00");
        resenia(ana, "Palermo", 4, "2026-11-15T23:45:00");
        // Lunes 00:30: es lunes.
        resenia(ana, "Palermo", 4, "2026-11-09T00:30:00");
        resenia(ana, "Palermo", 4, "2026-11-16T00:30:00");

        TuMesDto.Dia dia = servicio().resumen(ana.getId(), YearMonth.of(2026, 11)).dia();

        // Dos domingos y dos lunes: gana el lunes (día 1). La franja de las 00:30 es de madrugada.
        assertThat(dia).isEqualTo(new TuMesDto.Dia(1, "madrugada", 2));
    }

    @Test
    void conMenosDeTresReseniasNoHayDiaFavorito() {
        User ana = cuenta("tumes_dia_poco");
        resenia(ana, "Palermo", 4, "2026-11-05T21:00:00");
        resenia(ana, "Palermo", 4, "2026-11-12T22:30:00");

        assertThat(servicio().resumen(ana.getId(), YearMonth.of(2026, 11)).dia()).isNull();
    }

    private void reaccion(Rating resenia, User quien, TipoDeReaccion tipo, String enBuenosAires) {
        Instant cuando = ZonedDateTime.of(LocalDateTime.parse(enBuenosAires), TuMes.ZONA).toInstant();
        reaccionRepository.save(Reaccion.builder().rating(resenia).user(quien).tipo(tipo).createdAt(cuando).build());
    }

    @Test
    void cuentaLasReaccionesRecibidasEnElPeriodoYDiceCualFueLaMasUsada() {
        User ana = cuenta("tumes_reac");
        User beto = cuenta("tumes_reac_b");
        User carla = cuenta("tumes_reac_c");
        Rating r = resenia(ana, "Palermo", 4, "2026-11-05T20:00:00");
        reaccion(r, beto, TipoDeReaccion.ENCANTA, "2026-11-06T10:00:00");
        reaccion(r, carla, TipoDeReaccion.ENCANTA, "2026-11-07T10:00:00");
        reaccion(r, cuenta("tumes_reac_d"), TipoDeReaccion.HAMBRE, "2026-11-08T10:00:00");
        reaccion(r, cuenta("tumes_reac_e"), TipoDeReaccion.RISA, "2026-10-31T23:30:00");   // de octubre
        reaccion(r, cuenta("tumes_reac_f"), TipoDeReaccion.RISA, "2026-12-01T00:30:00");   // de diciembre
        reaccion(r, ana, TipoDeReaccion.HAMBRE, "2026-11-09T10:00:00");                    // la propia no cuenta

        TuMesDto.Reacciones reacciones = servicio().resumen(ana.getId(), YearMonth.of(2026, 11)).reacciones();

        assertThat(reacciones).isEqualTo(new TuMesDto.Reacciones(3, "ENCANTA"));
    }

    @Test
    void lasReaccionesDeQuienBloqueasONoTeBloqueanNoCuentanEnNingunSentido() {
        User ana = cuenta("tumes_bloq");
        User yoBloqueo = cuenta("tumes_bloq_a");
        User meBloquea = cuenta("tumes_bloq_b");
        User normal = cuenta("tumes_bloq_c");
        Rating r = resenia(ana, "Palermo", 4, "2026-11-05T20:00:00");
        reaccion(r, yoBloqueo, TipoDeReaccion.HAMBRE, "2026-11-06T10:00:00");
        reaccion(r, meBloquea, TipoDeReaccion.HAMBRE, "2026-11-06T11:00:00");
        reaccion(r, normal, TipoDeReaccion.RISA, "2026-11-06T12:00:00");
        blockRepository.save(Block.builder().blocker(ana).blocked(yoBloqueo).build());
        blockRepository.save(Block.builder().blocker(meBloquea).blocked(ana).build());

        TuMesDto.Reacciones reacciones = servicio().resumen(ana.getId(), YearMonth.of(2026, 11)).reacciones();

        assertThat(reacciones).isEqualTo(new TuMesDto.Reacciones(1, "RISA"));
    }

    @Test
    void sinReaccionesNoHayTarjeta() {
        User ana = cuenta("tumes_sinreac");
        resenia(ana, "Palermo", 4, "2026-11-05T20:00:00");

        assertThat(servicio().resumen(ana.getId(), YearMonth.of(2026, 11)).reacciones()).isNull();
    }


    private void logro(User quien, String codigo, String enBuenosAires, boolean activo, boolean retroactivo, String detalle) {
        Instant cuando = ZonedDateTime.of(LocalDateTime.parse(enBuenosAires), TuMes.ZONA).toInstant();
        logroRepository.save(new LogroDeUsuario(new LogroDeUsuarioId(quien.getId(), codigo), cuando, activo, cuando, null, retroactivo, detalle));
    }

    @Test
    void losLogrosDelMesSonLosGanadosEnElPeriodoEnHoraDeBuenosAires() {
        User ana = cuenta("tumes_logros");
        resenia(ana, "Palermo", 4, "2026-11-15T20:00:00");
        logro(ana, "NIVEL_3", "2026-10-31T23:30:00", true, false, null);       // octubre
        logro(ana, "NIVEL_5", "2026-11-01T00:30:00", true, false, null);       // noviembre, primer borde
        logro(ana, "INTENDENTE", "2026-11-30T23:30:00", true, false, "Palermo"); // noviembre, último borde
        logro(ana, "NIVEL_10", "2026-12-01T00:30:00", true, false, null);      // diciembre
        logro(ana, "NIVEL_15", "2026-11-10T12:00:00", false, false, null);     // lo perdió: no cuenta
        logro(ana, "NIVEL_20", "2026-11-10T12:00:00", true, true, null);       // de la carga inicial: no cuenta

        TuMesDto resumen = servicio().resumen(ana.getId(), YearMonth.of(2026, 11));

        assertThat(resumen.logros()).isEqualTo(2);
        assertThat(resumen.logrosGanados()).extracting(TuMesDto.LogroGanado::codigo).containsExactly("NIVEL_5", "INTENDENTE");
        assertThat(resumen.logrosGanados().get(1).detalle()).isEqualTo("Palermo");
    }

    @Test
    void sinLogrosEnElMesLaListaVieneVacia() {
        User ana = cuenta("tumes_sinlogros");
        resenia(ana, "Palermo", 4, "2026-11-15T20:00:00");

        assertThat(servicio().resumen(ana.getId(), YearMonth.of(2026, 11)).logrosGanados()).isEmpty();
    }

    private void podio(User quien, String periodo, LocalDate desde, int puesto) {
        podioRepository.save(new ResultadoDePodio(new ResultadoDePodio.Id(periodo, desde, puesto), quien.getId(), 100));
    }

    @Test
    void losPodiosDelMesSonLosQueEmpezaronEnElMesYSoloDeEstaPersona() {
        User ana = cuenta("tumes_podio");
        User beto = cuenta("tumes_podio_b");
        resenia(ana, "Palermo", 4, "2026-11-15T20:00:00");
        podio(ana, "semana", LocalDate.of(2026, 10, 26), 2);   // la semana que empezó en octubre
        podio(ana, "semana", LocalDate.of(2026, 11, 2), 1);
        podio(ana, "semana", LocalDate.of(2026, 11, 30), 3);   // lunes 30: empieza en noviembre
        podio(ana, "semana", LocalDate.of(2026, 12, 7), 1);    // diciembre
        podio(ana, "mes", LocalDate.of(2026, 11, 1), 2);
        podio(beto, "semana", LocalDate.of(2026, 11, 9), 1);   // de otra persona: no aparece

        TuMesDto resumen = servicio().resumen(ana.getId(), YearMonth.of(2026, 11));

        assertThat(resumen.podios())
            .extracting(TuMesDto.Puesto::periodo, TuMesDto.Puesto::desde, TuMesDto.Puesto::puesto)
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple("mes", LocalDate.of(2026, 11, 1), 2),
                org.assertj.core.groups.Tuple.tuple("semana", LocalDate.of(2026, 11, 2), 1),
                org.assertj.core.groups.Tuple.tuple("semana", LocalDate.of(2026, 11, 30), 3));
    }

    @Test
    void sinPodiosLaListaVieneVaciaYPedirloDeNuevoDaLoMismo() {
        User ana = cuenta("tumes_sinpodio");
        resenia(ana, "Palermo", 4, "2026-11-15T20:00:00");
        logro(ana, "NIVEL_5", "2026-11-05T12:00:00", true, false, null);

        TuMesDto primero = servicio().resumen(ana.getId(), YearMonth.of(2026, 11));
        TuMesDto segundo = servicio().resumen(ana.getId(), YearMonth.of(2026, 11));

        assertThat(primero.podios()).isEmpty();
        assertThat(segundo).isEqualTo(primero);
    }

    @Test
    void elDiaFavoritoEsCorrectoParaCadaDiaDeLaSemana() {
        // Del lunes 2 al domingo 8 de noviembre de 2026: tres reseñas al mediodía el mismo día.
        for (int dia = 2; dia <= 8; dia++) {
            User ana = cuenta("tumes_semana_" + dia);
            for (int hora = 12; hora <= 14; hora++) {
                resenia(ana, "Palermo", 4, String.format("2026-11-%02dT%02d:00:00", dia, hora));
            }

            TuMesDto.Dia favorito = servicio().resumen(ana.getId(), YearMonth.of(2026, 11)).dia();

            assertThat(favorito).as("día %d de noviembre", dia).isEqualTo(new TuMesDto.Dia(dia - 1, "tarde", 3));
        }
    }

    @Test
    void enElEmpateDeReaccionesGanaLaPrimeraDelCatalogo() {
        User ana = cuenta("tumes_empate_reac");
        Rating r = resenia(ana, "Palermo", 4, "2026-11-05T20:00:00");
        reaccion(r, cuenta("tumes_empate_a"), TipoDeReaccion.ENCANTA, "2026-11-06T10:00:00");
        reaccion(r, cuenta("tumes_empate_b"), TipoDeReaccion.HAMBRE, "2026-11-06T11:00:00");

        assertThat(servicio().resumen(ana.getId(), YearMonth.of(2026, 11)).reacciones())
            .isEqualTo(new TuMesDto.Reacciones(2, "HAMBRE"));
    }
}
