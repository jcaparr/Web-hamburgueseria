package com.hamburguesas.service;

import com.hamburguesas.dto.BurgerJointDto;
import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.model.Rating;
import com.hamburguesas.model.User;
import com.hamburguesas.model.WishlistItem;
import com.hamburguesas.repository.BurgerJointRepository;
import com.hamburguesas.repository.RatingRepository;
import com.hamburguesas.repository.UserRepository;
import com.hamburguesas.repository.WishlistRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Las fichas de varios locales armadas de una vez (#101).
 *
 * Antes cada local de la página pedía por separado su nota, cuántas reseñas tiene y si
 * está en la lista de deseos. Ahora son dos consultas para toda la página, y lo que se
 * prueba es que cada local siga recibiendo lo suyo y no lo del de al lado.
 *
 * Contra una base de verdad, porque lo que cambió son las consultas.
 */
@SpringBootTest
@ActiveProfiles("test")
class FichaDeLocalTest {

    @Autowired private FichaDeLocal fichaDeLocal;
    @Autowired private RatingRepository ratingRepository;
    @Autowired private WishlistRepository wishlistRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private BurgerJointRepository burgerJointRepository;

    private User ana;
    private User beto;
    private BurgerJoint conDos;
    private BurgerJoint conUna;
    private BurgerJoint sinNinguna;

    /**
     * Las personas y los locales se buscan antes de crearlos, en vez de borrar esas
     * tablas: otras clases de test comparten esta base. Lo que se limpia, antes y
     * después, son solo las reseñas y los guardados de estos tres locales.
     */
    @BeforeEach
    void setUp() {
        ana = alguien("anafichas");
        beto = alguien("betofichas");
        conDos = unLocal("fichas-con-dos", "Con dos");
        conUna = unLocal("fichas-con-una", "Con una");
        sinNinguna = unLocal("fichas-sin-ninguna", "Sin ninguna");
        limpiar();

        resenia(ana, conDos, 5);
        resenia(beto, conDos, 3);
        resenia(ana, conUna, 4);
        wishlistRepository.save(WishlistItem.builder().user(ana).burgerJoint(conUna).build());
    }

    @AfterEach
    void tearDown() {
        limpiar();
    }

    private void limpiar() {
        Set<Long> locales = Set.of(conDos.getId(), conUna.getId(), sinNinguna.getId());
        ratingRepository.deleteAll(ratingRepository.findAll().stream()
            .filter(r -> locales.contains(r.getBurgerJoint().getId())).toList());
        wishlistRepository.deleteAll(wishlistRepository.findAll().stream()
            .filter(w -> locales.contains(w.getBurgerJoint().getId())).toList());
    }

    private User alguien(String username) {
        return userRepository.findByUsername(username).orElseGet(() -> userRepository.save(
            User.builder().username(username).email(username + "@example.com")
                .emailVerified(true).build()));
    }

    private BurgerJoint unLocal(String placeId, String nombre) {
        return burgerJointRepository.findByPlaceId(placeId)
            .orElseGet(() -> burgerJointRepository.save(BurgerJoint.builder()
                .name(nombre).address("Una calle 123").area("Palermo").placeId(placeId).build()));
    }

    private void resenia(User de, BurgerJoint local, int nota) {
        ratingRepository.save(Rating.builder()
            .user(de).burgerJoint(local).score(nota).comment("algo").build());
    }

    @Test
    void cadaLocalRecibeSuNotaYSuCuentaEnElOrdenEnQueVino() {
        List<BurgerJointDto> fichas = fichaDeLocal.para(List.of(sinNinguna, conDos, conUna), null);

        assertThat(fichas).extracting(BurgerJointDto::name)
            .containsExactly("Sin ninguna", "Con dos", "Con una");
        assertThat(fichas.get(0).averageScore()).isNull();
        assertThat(fichas.get(0).ratingsCount()).isZero();
        assertThat(fichas.get(1).averageScore()).isEqualTo(4.0);
        assertThat(fichas.get(1).ratingsCount()).isEqualTo(2);
        assertThat(fichas.get(2).averageScore()).isEqualTo(4.0);
        assertThat(fichas.get(2).ratingsCount()).isEqualTo(1);
    }

    @Test
    void marcaGuardadasSoloLasDeQuienMira() {
        assertThat(fichaDeLocal.para(List.of(conDos, conUna), ana.getId()))
            .extracting(BurgerJointDto::inWishlist).containsExactly(false, true);
        assertThat(fichaDeLocal.para(List.of(conDos, conUna), beto.getId()))
            .extracting(BurgerJointDto::inWishlist).containsExactly(false, false);
        assertThat(fichaDeLocal.para(List.of(conDos, conUna), null))
            .extracting(BurgerJointDto::inWishlist).containsExactly(false, false);
    }

    @Test
    void unSoloLocalSeArmaIgualQueEnLaLista() {
        BurgerJointDto ficha = fichaDeLocal.para(conUna, ana.getId());

        assertThat(ficha.averageScore()).isEqualTo(4.0);
        assertThat(ficha.ratingsCount()).isEqualTo(1);
        assertThat(ficha.inWishlist()).isTrue();
    }

    /** Una página vacía no consulta nada: "in ()" ni siquiera es válido en todas las bases. */
    @Test
    void unaListaVaciaDevuelveUnaListaVacia() {
        assertThat(fichaDeLocal.para(List.of(), ana.getId())).isEmpty();
        assertThat(fichaDeLocal.conDeseo(List.of(), true)).isEmpty();
    }
}
