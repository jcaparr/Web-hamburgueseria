package com.hamburguesas.logros;

import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.model.Follow;
import com.hamburguesas.model.Rating;
import com.hamburguesas.model.Reaccion;
import com.hamburguesas.model.TipoDeReaccion;
import com.hamburguesas.model.User;
import com.hamburguesas.repository.BurgerJointRepository;
import com.hamburguesas.repository.FollowRepository;
import com.hamburguesas.repository.RatingRepository;
import com.hamburguesas.repository.ReaccionRepository;
import com.hamburguesas.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El motor de logros contra una base de verdad (#314): lo que mide con consultas, qué se
 * pierde al borrar, que el aviso no se repita y la carga inicial.
 */
@SpringBootTest
@ActiveProfiles("test")
class LogrosEnLaBaseTest {

    private static final String LARGO =
        "Muy buena la carne, el pan estaba tostado justo y la salsa me encantó bastante; las papas también salieron crocantes";

    @Autowired private Logros logros;
    @Autowired private RecalculoDeLogros recalculoDeLogros;
    @Autowired private LogroDeUsuarioRepository logroRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private BurgerJointRepository burgerJointRepository;
    @Autowired private RatingRepository ratingRepository;
    @Autowired private FollowRepository followRepository;
    @Autowired private ReaccionRepository reaccionRepository;

    private final List<User> creados = new ArrayList<>();

    @BeforeEach
    void limpiar() {
        borrarTodo();
    }

    @AfterEach
    void limpiarAlTerminar() {
        borrarTodo();
    }

    private void borrarTodo() {
        reaccionRepository.deleteAll();
        followRepository.deleteAll();
        ratingRepository.deleteAll();
        logroRepository.deleteAll();
        creados.clear();
    }

    private User cuenta(String nombre) {
        User u = userRepository.findByUsername(nombre).orElseGet(() -> userRepository.save(
            User.builder().username(nombre).email(nombre + "@example.com").emailVerified(true).build()));
        creados.add(u);
        return u;
    }

    private BurgerJoint local(String id, String area, boolean cadena) {
        return burgerJointRepository.findByPlaceId(id).orElseGet(() -> burgerJointRepository.save(
            BurgerJoint.builder().name("Local " + id).address("Calle 1").area(area).fastFood(cadena)
                .slug("logros-" + id).placeId(id).build()));
    }

    private Rating resenia(User quien, BurgerJoint donde, String comentario, Instant cuando) {
        return ratingRepository.save(Rating.builder().user(quien).burgerJoint(donde).score(4)
            .comment(comentario).createdAt(cuando).build());
    }

    private Set<Logro> activos(User u) {
        Set<Logro> set = new java.util.HashSet<>();
        logroRepository.deLaCuenta(u.getId()).stream().filter(LogroDeUsuario::isActivo).forEach(l -> set.add(l.logro()));
        return set;
    }

    private LogroDeUsuario fila(User u, Logro logro) {
        return logroRepository.findById(new LogroDeUsuarioId(u.getId(), logro.name())).orElse(null);
    }

    @Test
    void unaCuentaNuevaRecibeElSliderYRecalcularEsIdempotente() {
        User ana = cuenta("logros_ana");

        logros.recalcular(ana.getId(), false);
        Instant ganado = fila(ana, Logro.NIVEL_1).getGanadoEl();
        logros.recalcular(ana.getId(), false);

        assertThat(activos(ana)).contains(Logro.NIVEL_1);
        assertThat(logroRepository.deLaCuenta(ana.getId())).hasSize(activos(ana).size());
        assertThat(fila(ana, Logro.NIVEL_1).getGanadoEl()).isEqualTo(ganado);
    }

    @Test
    void losSeguidoresLosDanLosDemasYQuedanUnaVezGanados() {
        User famosa = cuenta("logros_famosa");
        List<User> fans = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            fans.add(cuenta("logros_fan" + i));
        }
        fans.forEach(fan -> followRepository.save(Follow.builder().follower(fan).followed(famosa).build()));

        logros.recalcular(famosa.getId(), false);
        assertThat(activos(famosa)).contains(Logro.SEGUIDORES_10);

        // Si la dejan de seguir, el logro queda: es de los que da otra gente.
        followRepository.deleteAll();
        logros.recalcular(famosa.getId(), false);
        assertThat(fila(famosa, Logro.SEGUIDORES_10).isActivo()).isTrue();
    }

    @Test
    void elIntendentePasaDeManoYSeGuardaElBarrio() {
        User ana = cuenta("logros_intendenta_a");
        User beto = cuenta("logros_intendente_b");
        BurgerJoint uno = local("lg-i1", "Palermo", false);
        BurgerJoint dos = local("lg-i2", "Palermo", false);
        BurgerJoint tres = local("lg-i3", "Palermo", false);
        Instant t = Instant.parse("2026-10-01T12:00:00Z");
        resenia(ana, uno, "ok", t);
        resenia(ana, dos, "ok", t.plusSeconds(10));
        resenia(beto, uno, "ok", t.plusSeconds(20));

        // Ana tiene 2 y Beto 1: Ana es intendenta de Palermo.
        logros.recalcular(ana.getId(), false);
        logros.recalcular(beto.getId(), false);
        assertThat(activos(ana)).contains(Logro.INTENDENTE);
        assertThat(fila(ana, Logro.INTENDENTE).getDetalle()).isEqualTo("Palermo");
        assertThat(activos(beto)).doesNotContain(Logro.INTENDENTE);

        // Beto llega a 2: empatan y gana quien llegó primero a esa cantidad, que es Ana.
        resenia(beto, dos, "ok", t.plusSeconds(30));
        logros.recalcular(ana.getId(), false);
        logros.recalcular(beto.getId(), false);
        assertThat(activos(ana)).contains(Logro.INTENDENTE);
        assertThat(activos(beto)).doesNotContain(Logro.INTENDENTE);

        // Beto pasa a 3: le gana a Ana, que lo pierde sin que se borre nada.
        resenia(beto, tres, "ok", t.plusSeconds(40));
        logros.recalcular(ana.getId(), false);
        logros.recalcular(beto.getId(), false);
        assertThat(activos(beto)).contains(Logro.INTENDENTE);
        assertThat(fila(ana, Logro.INTENDENTE).isActivo()).isFalse();
    }

    @Test
    void lasCadenasNoCuentanParaElIntendente() {
        User ana = cuenta("logros_cadena_a");
        BurgerJoint c1 = local("lg-c1", "Palermo", true);
        BurgerJoint c2 = local("lg-c2", "Palermo", true);
        resenia(ana, c1, "ok", Instant.now());
        resenia(ana, c2, "ok", Instant.now());

        logros.recalcular(ana.getId(), false);

        assertThat(activos(ana)).doesNotContain(Logro.INTENDENTE, Logro.BARRIOS_5);
    }

    @Test
    void lasReaccionesLasDanLosDemasYDiezEnUnaReseniaDanHambreAjenaYCombo() {
        User ana = cuenta("logros_reac_a");
        BurgerJoint local = local("lg-r1", "Almagro", false);
        Rating suya = resenia(ana, local, LARGO, Instant.now());
        TipoDeReaccion[] tipos = TipoDeReaccion.values();
        for (int i = 0; i < 10; i++) {
            User fan = cuenta("logros_reac_f" + i);
            reaccionRepository.save(Reaccion.builder().rating(suya).user(fan).tipo(tipos[i % tipos.length]).build());
        }

        logros.recalcular(ana.getId(), false);

        assertThat(activos(ana)).contains(Logro.HAMBRE_AJENA, Logro.COMBO);
    }

    @Test
    void bocaEnBocaSonTresSeguidoresQueReseniaronDespues() {
        User ana = cuenta("logros_boca_a");
        BurgerJoint local = local("lg-b1", "Boedo", false);
        Instant t = Instant.parse("2026-10-01T12:00:00Z");
        resenia(ana, local, "ok", t);
        for (int i = 0; i < 3; i++) {
            User f = cuenta("logros_boca_f" + i);
            followRepository.save(Follow.builder().follower(f).followed(ana).build());
            resenia(f, local, "ok", t.plusSeconds(60 + i));
        }

        logros.recalcular(ana.getId(), false);

        assertThat(activos(ana)).contains(Logro.BOCA_EN_BOCA);
    }

    @Test
    void borrarLaReseniaApagaLosPropiosPeroNoLosAjenos() {
        User ana = cuenta("logros_borra_a");
        BurgerJoint local = local("lg-p1", "Caballito", false);
        Rating suya = resenia(ana, local, LARGO, Instant.now());
        User fan = cuenta("logros_borra_f");
        reaccionRepository.save(Reaccion.builder().rating(suya).user(fan).tipo(TipoDeReaccion.RISA).build());
        logros.recalcular(ana.getId(), false);
        assertThat(activos(ana)).contains(Logro.PRIMERA_RESENIA);

        ratingRepository.delete(suya);
        logros.recalcular(ana.getId(), false);

        assertThat(fila(ana, Logro.PRIMERA_RESENIA).isActivo()).isFalse();
        assertThat(fila(ana, Logro.NIVEL_1).isActivo()).isTrue();
    }

    @Test
    void perderYVolverAGanarNoRepiteElAviso() {
        User ana = cuenta("logros_aviso_a");
        BurgerJoint local = local("lg-a1", "Flores", false);
        Rating suya = resenia(ana, local, "ok", Instant.now());
        logros.recalcular(ana.getId(), false);
        LogroDeUsuario primera = fila(ana, Logro.PRIMERA_RESENIA);
        assertThat(primera.getVistoEl()).isNull();
        primera.setVistoEl(Instant.parse("2026-10-02T10:00:00Z"));
        logroRepository.save(primera);
        Instant ganado = primera.getGanadoEl();

        ratingRepository.delete(suya);
        logros.recalcular(ana.getId(), false);
        resenia(ana, local, "ok", Instant.now());
        logros.recalcular(ana.getId(), false);

        LogroDeUsuario vuelta = fila(ana, Logro.PRIMERA_RESENIA);
        assertThat(vuelta.isActivo()).isTrue();
        assertThat(vuelta.getVistoEl()).isEqualTo(Instant.parse("2026-10-02T10:00:00Z"));
        assertThat(vuelta.getGanadoEl()).isEqualTo(ganado);
    }

    @Test
    void laCargaInicialMarcaComoRetroactivoLoQueYaMerecian() {
        User ana = cuenta("logros_carga_a");
        resenia(ana, local("lg-k1", "Retiro", false), LARGO, Instant.now());
        assertThat(logroRepository.count()).isZero();

        recalculoDeLogros.run(null);

        assertThat(fila(ana, Logro.PRIMERA_RESENIA).isRetroactivo()).isTrue();
        assertThat(fila(ana, Logro.PRIMERA_RESENIA).getVistoEl()).isNull();

        // La carga es por cuenta: una que ya tiene logros no se toca, y una sin ninguno la recibe.
        Instant antes = fila(ana, Logro.PRIMERA_RESENIA).getGanadoEl();
        User beto = cuenta("logros_carga_b");
        recalculoDeLogros.run(null);
        assertThat(fila(ana, Logro.PRIMERA_RESENIA).getGanadoEl()).isEqualTo(antes);
        assertThat(fila(beto, Logro.NIVEL_1).isRetroactivo()).isTrue();
    }

    @Test
    void despuesDeLaCargaLoNuevoNoEsRetroactivo() {
        User ana = cuenta("logros_nuevo_a");
        logros.recalcular(ana.getId(), false);
        assertThat(fila(ana, Logro.NIVEL_1).isRetroactivo()).isFalse();
    }

    @Test
    void elEventoDeRecalculoSeAtiendeSolo() {
        User ana = cuenta("logros_evento_a");
        recalculoDeLogros.alCambiar(LogrosPorRecalcular.de(ana.getId()));
        assertThat(activos(ana)).contains(Logro.NIVEL_1);
    }
}
