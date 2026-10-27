package com.hamburguesas.service;

import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.model.Rating;
import com.hamburguesas.model.User;
import com.hamburguesas.repository.BurgerJointRepository;
import com.hamburguesas.repository.RatingRepository;
import com.hamburguesas.repository.TareaUnicaRepository;
import com.hamburguesas.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El recálculo único de la XP con la regla de 5 palabras (#344): sube lo que corresponde,
 * no baja nada, y correrlo de nuevo no cambia nada.
 */
@SpringBootTest
@ActiveProfiles("test")
class RecalculoUnicoDeXpTest {

    @Autowired private RecalculoUnicoDeXp recalculo;
    @Autowired private UserRepository userRepository;
    @Autowired private BurgerJointRepository burgerJointRepository;
    @Autowired private RatingRepository ratingRepository;
    @Autowired private TareaUnicaRepository tareas;
    @Autowired private org.springframework.transaction.support.TransactionTemplate transacciones;

    @BeforeEach
    @AfterEach
    void limpiar() {
        ratingRepository.deleteAll();
        tareas.deleteAll();
    }

    private User cuenta(String nombre, int xp) {
        User u = userRepository.findByUsername(nombre).orElseGet(() -> userRepository.save(
            User.builder().username(nombre).email(nombre + "@example.com").emailVerified(true).build()));
        return ponerXp(u, xp);
    }

    /** La XP guardada, tal como la dejaría la regla vieja. */
    private User ponerXp(User u, int xp) {
        // La columna no se escribe desde la entidad (la deja RecalculoDeXp): va por la consulta.
        transacciones.executeWithoutResult(estado -> userRepository.ponerXp(u.getId(), xp));
        return u;
    }

    private BurgerJoint local(String id) {
        return burgerJointRepository.findByPlaceId(id).orElseGet(() -> burgerJointRepository.save(
            BurgerJoint.builder().name("Local " + id).address("Calle 1").slug("xp5-" + id).placeId(id).build()));
    }

    private void resenia(User u, BurgerJoint l, String comentario) {
        ratingRepository.save(Rating.builder().user(u).burgerJoint(l).score(4).comment(comentario).build());
    }

    private int xpDe(User u) {
        return userRepository.findById(u.getId()).orElseThrow().getXp();
    }

    @Test
    void unComentarioDeCincoANueveLetrasPasaDeSetentaACien() {
        User ana = cuenta("xp5_ana", 100); // 70 de la reseña + 30 de bienvenida, con la regla vieja
        resenia(ana, local("xp5-1"), "muy buena la carne eh");

        RecalculoUnicoDeXp.Resultado resultado = recalculo.ejecutar();

        assertThat(xpDe(ana)).isEqualTo(130); // 100 + 30
        assertThat(resultado.subieron()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void correrloDosVecesNoCambiaNada() {
        User ana = cuenta("xp5_beto", 100);
        resenia(ana, local("xp5-2"), "muy buena la carne eh");

        recalculo.ejecutar();
        int despuesDeLaPrimera = xpDe(ana);
        RecalculoUnicoDeXp.Resultado segunda = recalculo.ejecutar();

        assertThat(xpDe(ana)).isEqualTo(despuesDeLaPrimera);
        assertThat(segunda.subieron()).isZero();
        assertThat(segunda.conError()).isZero();
    }

    @Test
    void nuncaBajaLaXp() {
        // Una cuenta con más XP guardada que la que dan sus reseñas: se deja como está.
        User caro = cuenta("xp5_caro", 500);
        resenia(caro, local("xp5-3"), "ok");

        RecalculoUnicoDeXp.Resultado resultado = recalculo.ejecutar();

        assertThat(xpDe(caro)).isEqualTo(500);
        assertThat(resultado.sinTocar()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void sinReseniasNoSeToca() {
        User sin = cuenta("xp5_sin", 0);

        recalculo.ejecutar();

        assertThat(xpDe(sin)).isZero();
    }

    @Test
    void alTerminarSeAnotaYNoSeRepiteAlArrancar() {
        User ana = cuenta("xp5_carla", 100);
        resenia(ana, local("xp5-4"), "muy buena la carne eh");

        recalculo.run(new DefaultApplicationArguments());
        assertThat(tareas.existsById(RecalculoUnicoDeXp.NOMBRE)).isTrue();
        assertThat(xpDe(ana)).isEqualTo(130);

        // Con la marca puesta no recorre las cuentas: una XP vieja queda como está.
        ponerXp(ana, 100);
        recalculo.run(new DefaultApplicationArguments());
        assertThat(xpDe(ana)).isEqualTo(100);
    }
}
