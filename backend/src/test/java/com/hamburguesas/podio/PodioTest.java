package com.hamburguesas.podio;

import com.hamburguesas.logros.Logro;
import com.hamburguesas.logros.LogroDeUsuarioId;
import com.hamburguesas.logros.LogroDeUsuarioRepository;
import com.hamburguesas.model.Block;
import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.model.Rating;
import com.hamburguesas.model.User;
import com.hamburguesas.repository.BlockRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** El podio de personas: XP por período, desempates y cierres con sus logros (#318). */
@SpringBootTest
@ActiveProfiles("test")
class PodioTest {

    private static final String LARGO =
        "Muy buena la carne, el pan estaba tostado justo y la salsa me encantó bastante; las papas también salieron crocantes";
    /** Lunes 7 de septiembre de 2026, 12:00 en Buenos Aires = 15:00 UTC. */
    private static final LocalDate LUNES = LocalDate.of(2026, 9, 7);
    private static final Instant LUNES_AL_MEDIODIA = Instant.parse("2026-09-07T15:00:00Z");

    @Autowired private Podio podio;
    @Autowired private UserRepository userRepository;
    @Autowired private BurgerJointRepository burgerJointRepository;
    @Autowired private RatingRepository ratingRepository;
    @Autowired private ReaccionRepository reaccionRepository;
    @Autowired private LogroDeUsuarioRepository logroRepository;
    @Autowired private ResultadoDePodioRepository resultados;
    @Autowired private BlockRepository blockRepository;

    private int contador;

    @BeforeEach
    void setUp() {
        limpiar();
    }

    @AfterEach
    void limpiar() {
        blockRepository.deleteAll();
        reaccionRepository.deleteAll();
        ratingRepository.deleteAll();
        logroRepository.deleteAll();
        resultados.deleteAll();
    }

    private User cuenta(String nombre) {
        return userRepository.findByUsername(nombre).orElseGet(() -> userRepository.save(
            User.builder().username(nombre).email(nombre + "@example.com").emailVerified(true).build()));
    }

    private BurgerJoint local() {
        String id = "podio-" + (++contador);
        return burgerJointRepository.findByPlaceId(id).orElseGet(() -> burgerJointRepository.save(
            BurgerJoint.builder().name(id).address("c").area("Palermo").slug(id).placeId(id).build()));
    }

    private Rating resenia(User quien, String comentario, Instant cuando) {
        return ratingRepository.save(Rating.builder().user(quien).burgerJoint(local()).score(4)
            .comment(comentario).createdAt(cuando).build());
    }

    private List<Podio.Marca> semana() {
        return podio.ranking(LUNES.atStartOfDay(Podio.ZONA).toInstant(), LUNES.plusDays(7).atStartOfDay(Podio.ZONA).toInstant());
    }

    @Test
    void laXpDeLaSemanaSumaLasReseniasDeEsaSemanaMasLaBienvenidaEnLaPrimera() {
        User ana = cuenta("podio_ana");
        resenia(ana, LARGO, LUNES_AL_MEDIODIA);
        resenia(ana, "ok", LUNES_AL_MEDIODIA.plusSeconds(60));

        // 100 + 70 + 30 de bienvenida.
        assertThat(semana()).singleElement().satisfies(m -> {
            assertThat(m.userId()).isEqualTo(ana.getId());
            assertThat(m.xp()).isEqualTo(200);
        });
    }

    @Test
    void laBienvenidaCuentaEnElPeriodoDeLaPrimeraResenia() {
        User ana = cuenta("podio_ana2");
        resenia(ana, "ok", LUNES_AL_MEDIODIA.minusSeconds(7L * 24 * 3600)); // la semana anterior
        resenia(ana, "ok", LUNES_AL_MEDIODIA);

        // Esta semana: solo los 70 de la reseña, sin los 30.
        assertThat(semana()).singleElement().extracting(Podio.Marca::xp).isEqualTo(70);
    }

    @Test
    void sinXpEnElPeriodoNoAparece() {
        User ana = cuenta("podio_ana3");
        resenia(ana, "ok", LUNES_AL_MEDIODIA.minusSeconds(30L * 24 * 3600));

        assertThat(semana()).isEmpty();
    }

    @Test
    void borrarUnaReseniaLaSacaDelPeriodo() {
        User ana = cuenta("podio_ana4");
        Rating r = resenia(ana, LARGO, LUNES_AL_MEDIODIA);
        assertThat(semana()).hasSize(1);

        ratingRepository.delete(r);

        assertThat(semana()).isEmpty();
    }

    @Test
    void enElEmpateGanaQuienLlegoAntesALaXp() {
        User ana = cuenta("podio_empate_a");
        User beto = cuenta("podio_empate_b");
        resenia(beto, "ok", LUNES_AL_MEDIODIA.plusSeconds(3600));
        resenia(ana, "ok", LUNES_AL_MEDIODIA.plusSeconds(7200));

        assertThat(semana()).extracting(Podio.Marca::userId).containsExactly(beto.getId(), ana.getId());
    }

    @Test
    void ordenaDeMasAMenosXp() {
        User ana = cuenta("podio_orden_a");
        User beto = cuenta("podio_orden_b");
        resenia(ana, "ok", LUNES_AL_MEDIODIA);
        resenia(beto, LARGO, LUNES_AL_MEDIODIA);

        assertThat(semana()).extracting(Podio.Marca::userId).containsExactly(beto.getId(), ana.getId());
    }

    @Test
    void cerrarLaSemanaGuardaElTop3YDaElPodioSemanalSoloAEsos() {
        User[] gente = new User[4];
        for (int i = 0; i < 4; i++) {
            gente[i] = cuenta("podio_cierre_" + i);
            // Cada una con una reseña más que la siguiente en XP: la 0 la que más.
            for (int k = 0; k < 4 - i; k++) {
                resenia(gente[i], LARGO, LUNES_AL_MEDIODIA.plusSeconds(k));
            }
        }

        podio.cerrarSemana(LUNES);

        assertThat(resultados.de(Podio.SEMANA, LUNES)).hasSize(3);
        for (int i = 0; i < 3; i++) {
            assertThat(logroRepository.findById(new LogroDeUsuarioId(gente[i].getId(), Logro.PODIO_SEMANAL.name()))).isPresent();
        }
        assertThat(logroRepository.findById(new LogroDeUsuarioId(gente[3].getId(), Logro.PODIO_SEMANAL.name()))).isEmpty();
    }

    @Test
    void cerrarDosVecesNoHaceNada() {
        User ana = cuenta("podio_dos_veces");
        resenia(ana, LARGO, LUNES_AL_MEDIODIA);

        podio.cerrarSemana(LUNES);
        // Un cambio posterior no altera lo ya cerrado.
        ratingRepository.deleteAll();
        podio.cerrarSemana(LUNES);

        assertThat(resultados.de(Podio.SEMANA, LUNES)).singleElement()
            .satisfies(r -> assertThat(r.getUserId()).isEqualTo(ana.getId()));
    }

    @Test
    void primeroDelMesYRachaDeOroAlSerloDosMesesSeguidos() {
        User ana = cuenta("podio_racha_a");
        User beto = cuenta("podio_racha_b");
        // Agosto: gana ana. Septiembre: gana ana. Octubre: gana beto.
        resenia(ana, LARGO, Instant.parse("2026-08-10T15:00:00Z"));
        resenia(ana, LARGO, Instant.parse("2026-09-10T15:00:00Z"));
        resenia(ana, LARGO, Instant.parse("2026-09-12T15:00:00Z"));
        resenia(beto, LARGO, Instant.parse("2026-09-11T15:00:00Z"));
        resenia(beto, LARGO, Instant.parse("2026-10-10T15:00:00Z"));
        resenia(beto, LARGO, Instant.parse("2026-10-11T15:00:00Z"));

        podio.cerrarMes(YearMonth.of(2026, 8));
        assertThat(logroRepository.findById(new LogroDeUsuarioId(ana.getId(), Logro.PRIMERO_DEL_MES.name()))).isPresent();
        assertThat(logroRepository.findById(new LogroDeUsuarioId(ana.getId(), Logro.RACHA_DE_ORO.name()))).isEmpty();

        podio.cerrarMes(YearMonth.of(2026, 9));
        assertThat(logroRepository.findById(new LogroDeUsuarioId(ana.getId(), Logro.RACHA_DE_ORO.name()))).isPresent();
        assertThat(logroRepository.findById(new LogroDeUsuarioId(beto.getId(), Logro.RACHA_DE_ORO.name()))).isEmpty();

        podio.cerrarMes(YearMonth.of(2026, 10));
        assertThat(logroRepository.findById(new LogroDeUsuarioId(beto.getId(), Logro.PRIMERO_DEL_MES.name()))).isPresent();
        assertThat(logroRepository.findById(new LogroDeUsuarioId(beto.getId(), Logro.RACHA_DE_ORO.name()))).isEmpty();
    }

    @Test
    void unMesSinNadieCortaLaRachaDeOro() {
        User ana = cuenta("podio_sin_nadie");
        resenia(ana, LARGO, Instant.parse("2026-08-10T15:00:00Z"));
        resenia(ana, LARGO, Instant.parse("2026-10-10T15:00:00Z"));
        resenia(ana, LARGO, Instant.parse("2026-10-11T15:00:00Z"));

        podio.cerrarMes(YearMonth.of(2026, 8));
        podio.cerrarMes(YearMonth.of(2026, 9)); // nadie ganó XP en septiembre
        podio.cerrarMes(YearMonth.of(2026, 10));

        assertThat(logroRepository.findById(new LogroDeUsuarioId(ana.getId(), Logro.PRIMERO_DEL_MES.name()))).isPresent();
        assertThat(logroRepository.findById(new LogroDeUsuarioId(ana.getId(), Logro.RACHA_DE_ORO.name()))).isEmpty();
    }

    @Test
    void laSemanaVaDeLunesADomingoEnHoraDeBuenosAires() {
        User ana = cuenta("podio_borde");
        // Domingo 13 a las 23:30 en Buenos Aires (02:30 UTC del lunes 14): todavía es de esta semana.
        resenia(ana, "ok", Instant.parse("2026-09-14T02:30:00Z"));
        // Lunes 14 a las 00:30 en Buenos Aires (03:30 UTC): ya es de la siguiente.
        User beto = cuenta("podio_borde_b");
        resenia(beto, "ok", Instant.parse("2026-09-14T03:30:00Z"));

        assertThat(semana()).extracting(Podio.Marca::userId).containsExactly(ana.getId());
    }

    @Test
    void laBienvenidaNoSeDuplicaSiLaPrimeraYLaSegundaCaenEnElPeriodo() {
        User ana = cuenta("podio_bienv_unica");
        resenia(ana, "ok", LUNES_AL_MEDIODIA);
        resenia(ana, "ok", LUNES_AL_MEDIODIA.plusSeconds(60));
        resenia(ana, "ok", LUNES_AL_MEDIODIA.plusSeconds(120));

        // 3 x 70 + 30, una sola vez.
        assertThat(semana()).singleElement().extracting(Podio.Marca::xp).isEqualTo(240);
    }

    @Test
    @Transactional // la XP se escribe con un update dirigido; se deshace sola al terminar
    void elGeneralOrdenaPorLaXpGuardadaDeLaCuenta() {
        User ana = cuenta("podio_general_a");
        User beto = cuenta("podio_general_b");
        userRepository.ponerXp(ana.getId(), 300);
        userRepository.ponerXp(beto.getId(), 500);
        resenia(ana, "ok", LUNES_AL_MEDIODIA);
        resenia(beto, "ok", LUNES_AL_MEDIODIA);

        assertThat(podio.rankingGeneral()).extracting(Podio.Marca::userId)
            .filteredOn(id -> id.equals(ana.getId()) || id.equals(beto.getId()))
            .containsExactly(beto.getId(), ana.getId());
    }

    @Test
    void elPodioNoMuestraAQuienTieneUnBloqueoConQuienMira() {
        User ana = cuenta("podio_bloq_ana");
        User beto = cuenta("podio_bloq_beto");
        User carla = cuenta("podio_bloq_carla");
        Instant ahora = Instant.now();
        resenia(ana, LARGO, ahora.minusSeconds(30));
        resenia(beto, LARGO, ahora.minusSeconds(20));
        resenia(carla, LARGO, ahora.minusSeconds(10));
        blockRepository.save(Block.builder().blocker(ana).blocked(beto).build());

        // Ana bloqueó a Beto: ninguno de los dos ve al otro, Carla los ve a los dos.
        assertThat(podio.de(Podio.SEMANA, ana.getId()).top()).extracting(PodioDto.Puesto::userId)
            .contains(ana.getId(), carla.getId()).doesNotContain(beto.getId());
        assertThat(podio.de(Podio.SEMANA, beto.getId()).top()).extracting(PodioDto.Puesto::userId)
            .contains(beto.getId(), carla.getId()).doesNotContain(ana.getId());
        assertThat(podio.de(Podio.SEMANA, carla.getId()).top()).extracting(PodioDto.Puesto::userId)
            .contains(ana.getId(), beto.getId(), carla.getId());
    }

    @Test
    void sinReseniasEnElPeriodoNoHayPuestoPropio() {
        User ana = cuenta("podio_sin_puesto");

        assertThat(podio.de(Podio.SEMANA, ana.getId()).yo()).isNull();
    }
}
