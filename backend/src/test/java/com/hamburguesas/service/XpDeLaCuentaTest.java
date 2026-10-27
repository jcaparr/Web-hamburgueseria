package com.hamburguesas.service;

import com.hamburguesas.dto.ItemDeFeedDto;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La XP de una cuenta contra una base de verdad (#310): se recalcula de las reseñas, y
 * viaja hasta donde se dibuja el avatar.
 */
@SpringBootTest
@ActiveProfiles("test")
class XpDeLaCuentaTest {

    private static final String LARGO =
        "Muy buena la carne, el pan estaba tostado justo y la salsa me encantó bastante; las papas también salieron crocantes";

    @Autowired private RatingRepository ratingRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private BurgerJointRepository burgerJointRepository;
    @Autowired private RecalculoDeXp recalculo;
    @Autowired private ApplicationEventPublisher eventos;

    private User autor;
    private BurgerJoint uno;
    private BurgerJoint otro;

    @BeforeEach
    void setUp() {
        ratingRepository.deleteAll();
        autor = userRepository.findByUsername("xpdelacuenta").orElseGet(() -> userRepository.save(
            User.builder().username("xpdelacuenta").email("xpdelacuenta@example.com")
                .emailVerified(true).build()));
        uno = local("xp-uno");
        otro = local("xp-otro");
    }

    @AfterEach
    void tearDown() {
        ratingRepository.deleteAll();
        recalculo.recalcular(autor.getId());
    }

    private BurgerJoint local(String id) {
        return burgerJointRepository.findByPlaceId(id).orElseGet(() -> burgerJointRepository.save(
            BurgerJoint.builder().name(id).address("Una calle 123").area("Palermo")
                .slug(id).placeId(id).build()));
    }

    private Rating resenia(BurgerJoint donde, String comentario) {
        return ratingRepository.save(Rating.builder().user(autor).burgerJoint(donde).score(4)
            .comment(comentario).createdAt(Instant.parse("2026-09-20T15:00:00Z")).build());
    }

    private int xp() {
        return userRepository.findById(autor.getId()).orElseThrow().getXp();
    }

    @Test
    void unaCuentaNuevaArrancaEnCero() {
        assertThat(xp()).isZero();
    }

    @Test
    void conFotoSolaSonSetentaMasLaBienvenida() {
        resenia(uno, null);

        recalculo.recalcular(autor.getId());

        assertThat(xp()).isEqualTo(100);
    }

    @Test
    void conComentarioSonCienMasLaBienvenidaYLaBienvenidaNoSeRepite() {
        resenia(uno, LARGO);
        resenia(otro, "buena");

        recalculo.recalcular(autor.getId());

        assertThat(xp()).isEqualTo(100 + 70 + 30);
    }

    @Test
    void agregarleElComentarioLaPasaDeSetentaACien() {
        Rating r = resenia(uno, "buena");
        recalculo.recalcular(autor.getId());
        assertThat(xp()).isEqualTo(100);

        r.setComment(LARGO);
        ratingRepository.save(r);
        recalculo.recalcular(autor.getId());

        assertThat(xp()).isEqualTo(130);
    }

    @Test
    void borrarRestaLaXpYPuedeBajarDeNivel() {
        resenia(uno, LARGO);
        Rating segunda = resenia(otro, LARGO);
        recalculo.recalcular(autor.getId());
        assertThat(xp()).isEqualTo(230);
        assertThat(Niveles.nivelDe(xp())).isEqualTo(3);

        ratingRepository.delete(segunda);
        recalculo.recalcular(autor.getId());

        assertThat(xp()).isEqualTo(130);
        assertThat(Niveles.nivelDe(xp())).isEqualTo(2);
    }

    @Test
    void borrarLaUltimaDejaLaCuentaEnCeroSinBienvenida() {
        Rating unica = resenia(uno, LARGO);
        recalculo.recalcular(autor.getId());

        ratingRepository.delete(unica);
        recalculo.recalcular(autor.getId());

        assertThat(xp()).isZero();
    }

    /** El aviso de RatingService se atiende solo, aunque quien publique no tenga transacción. */
    @Test
    void elEventoDeLasReseniasRecalculaLaXp() {
        resenia(uno, LARGO);

        eventos.publishEvent(new XpPorRecalcular(autor.getId()));

        assertThat(xp()).isEqualTo(130);
    }

    @Test
    void recalcularDosVecesDejaElMismoNumero() {
        resenia(uno, LARGO);

        recalculo.recalcular(autor.getId());
        recalculo.recalcular(autor.getId());

        assertThat(xp()).isEqualTo(130);
    }

    /** Una entidad cargada antes del recálculo y guardada después no pisa el número. */
    @Test
    void guardarLaCuentaNoPisaLaXp() {
        resenia(uno, LARGO);
        User vieja = userRepository.findById(autor.getId()).orElseThrow();

        recalculo.recalcular(autor.getId());
        vieja.setHamburguesa("01111");
        userRepository.save(vieja);

        assertThat(xp()).isEqualTo(130);
    }

    @Test
    void elFeedTraeLaXpDelAutor() {
        resenia(uno, LARGO);
        recalculo.recalcular(autor.getId());

        List<ItemDeFeedDto> feed = ratingRepository.feedDeTodos(
            Instant.parse("9999-12-31T23:59:59Z"), Long.MAX_VALUE, List.of(-1L), PageRequest.of(0, 10));

        assertThat(feed).singleElement().extracting(ItemDeFeedDto::autorXp).isEqualTo(130);
    }
}
