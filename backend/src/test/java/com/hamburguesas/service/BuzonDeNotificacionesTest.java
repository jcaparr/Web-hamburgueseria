package com.hamburguesas.service;

import com.hamburguesas.dto.BuzonDto;
import com.hamburguesas.dto.NotificacionDto;
import com.hamburguesas.model.Block;
import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.model.Follow;
import com.hamburguesas.model.Rating;
import com.hamburguesas.model.Reaccion;
import com.hamburguesas.model.TipoDeReaccion;
import com.hamburguesas.model.User;
import com.hamburguesas.repository.BlockRepository;
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
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El buzón de notificaciones (#210): quién te siguió y quién reaccionó a tus reseñas.
 *
 * Contra una base de verdad, porque el buzón no tiene tabla propia: sale de las consultas
 * sobre los seguimientos y las reacciones. Las personas son propias de esta clase, y se
 * limpia solo lo de ellas: la base es compartida.
 */
@SpringBootTest
@ActiveProfiles("test")
class BuzonDeNotificacionesTest {

    private static final Instant HACE_UN_RATO = Instant.now().minus(3, ChronoUnit.HOURS);

    @Autowired private NotificacionesService service;
    @Autowired private UserRepository userRepository;
    @Autowired private FollowRepository followRepository;
    @Autowired private ReaccionRepository reaccionRepository;
    @Autowired private RatingRepository ratingRepository;
    @Autowired private BlockRepository blockRepository;
    @Autowired private BurgerJointRepository burgerJointRepository;

    private User yo;
    private User ana;
    private User beto;
    private User caro;
    private BurgerJoint local;
    private Rating miResenia;

    @BeforeEach
    void setUp() {
        yo = alguien("yobuzon");
        ana = alguien("anabuzon");
        beto = alguien("betobuzon");
        caro = alguien("carobuzon");
        local = burgerJointRepository.findByPlaceId("place-del-test-del-buzon")
            .orElseGet(() -> burgerJointRepository.save(BurgerJoint.builder()
                .name("Bmoodie").address("Beruti 3336").placeId("place-del-test-del-buzon").build()));
        limpiar();
        yo.setNotificacionesVistasEl(null);
        yo = userRepository.save(yo);
        miResenia = ratingRepository.save(Rating.builder().user(yo).burgerJoint(local).score(5).build());
    }

    @AfterEach
    void tearDown() {
        limpiar();
    }

    private void limpiar() {
        Set<Long> mios = Set.of(yo.getId(), ana.getId(), beto.getId(), caro.getId());
        reaccionRepository.deleteAll(reaccionRepository.findAll().stream()
            .filter(r -> mios.contains(r.getUser().getId())).toList());
        ratingRepository.deleteAll(ratingRepository.findAll().stream()
            .filter(r -> mios.contains(r.getUser().getId())).toList());
        followRepository.deleteAll(followRepository.findAll().stream()
            .filter(f -> mios.contains(f.getFollower().getId()) || mios.contains(f.getFollowed().getId())).toList());
        blockRepository.deleteAll(blockRepository.findAll().stream()
            .filter(b -> mios.contains(b.getBlocker().getId()) || mios.contains(b.getBlocked().getId())).toList());
    }

    private User alguien(String username) {
        return userRepository.findByUsername(username).orElseGet(() -> userRepository.save(
            User.builder().username(username).email(username + "@example.com").emailVerified(true).build()));
    }

    private void sigue(User quien, User aQuien, Instant cuando) {
        followRepository.save(Follow.builder().follower(quien).followed(aQuien).createdAt(cuando).build());
    }

    private void reacciona(User quien, TipoDeReaccion tipo, Instant cuando) {
        reaccionRepository.save(Reaccion.builder()
            .user(quien).rating(miResenia).tipo(tipo).createdAt(cuando).build());
    }

    @Test
    void mezclaSeguimientosYReaccionesDeLoMasNuevoALoMasViejo() {
        sigue(ana, yo, HACE_UN_RATO);
        reacciona(beto, TipoDeReaccion.FUEGO, HACE_UN_RATO.plus(1, ChronoUnit.HOURS));
        sigue(caro, yo, HACE_UN_RATO.plus(2, ChronoUnit.HOURS));

        List<NotificacionDto> avisos = service.buzon(yo.getId()).notificaciones();

        assertThat(avisos).extracting(NotificacionDto::username)
            .containsExactly("carobuzon", "betobuzon", "anabuzon");
        assertThat(avisos).extracting(NotificacionDto::tipo).containsExactly(
            NotificacionDto.Tipo.SEGUIMIENTO, NotificacionDto.Tipo.REACCION, NotificacionDto.Tipo.SEGUIMIENTO);
    }

    /** La reacción dice cuál fue y de qué local es la reseña, para llevar a su ficha. */
    @Test
    void laReaccionTraeElLocalDeLaResenia() {
        reacciona(beto, TipoDeReaccion.HAMBRE, HACE_UN_RATO);

        NotificacionDto aviso = service.buzon(yo.getId()).notificaciones().get(0);

        assertThat(aviso.reaccion()).isEqualTo(TipoDeReaccion.HAMBRE);
        assertThat(aviso.localId()).isEqualTo(local.getId());
        assertThat(aviso.localNombre()).isEqualTo("Bmoodie");
    }

    /**
     * Quien nunca abrió el buzón tiene todo como nuevo. Es lo que pasa con las cuentas de
     * antes del buzón: lo que nunca se les avisó les llega la primera vez.
     */
    @Test
    void siNuncaLoAbrioTodoEsNuevo() {
        sigue(ana, yo, HACE_UN_RATO);
        reacciona(beto, TipoDeReaccion.FUEGO, HACE_UN_RATO);

        BuzonDto buzon = service.buzon(yo.getId());

        assertThat(buzon.nuevas()).isEqualTo(2);
        assertThat(buzon.notificaciones()).allMatch(NotificacionDto::nueva);
        assertThat(service.nuevas(yo.getId()).nuevas()).isEqualTo(2);
    }

    /** Abrirlo deja la campana en cero, y lo que llega después vuelve a contar. */
    @Test
    void abrirloDejaLaCampanaEnCero() {
        sigue(ana, yo, HACE_UN_RATO);

        service.marcarVistas(yo.getId());

        assertThat(service.nuevas(yo.getId()).nuevas()).isZero();
        assertThat(service.buzon(yo.getId()).notificaciones()).noneMatch(NotificacionDto::nueva);

        sigue(beto, yo, Instant.now().plus(1, ChronoUnit.MINUTES));
        assertThat(service.nuevas(yo.getId()).nuevas()).isEqualTo(1);
    }

    /** Uno no se avisa a sí mismo: la base no impide reaccionar a lo propio. */
    @Test
    void lasReaccionesPropiasNoAvisan() {
        reacciona(yo, TipoDeReaccion.APLAUSO, HACE_UN_RATO);

        assertThat(service.buzon(yo.getId()).notificaciones()).isEmpty();
        assertThat(service.nuevas(yo.getId()).nuevas()).isZero();
    }

    /** Con un bloqueo de por medio, en cualquiera de las dos direcciones, no se ve. */
    @Test
    void conUnBloqueoNoSeVe() {
        sigue(ana, yo, HACE_UN_RATO);
        reacciona(beto, TipoDeReaccion.FUEGO, HACE_UN_RATO);
        blockRepository.save(Block.builder().blocker(yo).blocked(ana).build());
        blockRepository.save(Block.builder().blocker(beto).blocked(yo).build());

        assertThat(service.buzon(yo.getId()).notificaciones()).isEmpty();
        assertThat(service.nuevas(yo.getId()).nuevas()).isZero();
    }

    /** Si te deja de seguir, el aviso desaparece solo: no hay una tabla que limpiar. */
    @Test
    void dejarDeSeguirSacaElAviso() {
        sigue(ana, yo, HACE_UN_RATO);
        followRepository.deleteAll(followRepository.findAll().stream()
            .filter(f -> f.getFollower().getId().equals(ana.getId())).toList());

        assertThat(service.buzon(yo.getId()).notificaciones()).isEmpty();
    }

    /** Para el botón de seguir de vuelta: dice si ya la seguís. */
    @Test
    void diceSiYaLaSeguis() {
        sigue(ana, yo, HACE_UN_RATO);
        sigue(caro, yo, HACE_UN_RATO.plus(1, ChronoUnit.MINUTES));
        sigue(yo, caro, HACE_UN_RATO);

        List<NotificacionDto> avisos = service.buzon(yo.getId()).notificaciones();

        assertThat(avisos).filteredOn(a -> a.username().equals("carobuzon")).singleElement()
            .extracting(NotificacionDto::loSigo).isEqualTo(true);
        assertThat(avisos).filteredOn(a -> a.username().equals("anabuzon")).singleElement()
            .extracting(NotificacionDto::loSigo).isEqualTo(false);
    }
}
