package com.hamburguesas.repository;

import com.hamburguesas.dto.ItemDeFeedDto;
import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.model.Rating;
import com.hamburguesas.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Las dos consultas del feed, ejecutadas de verdad contra una base.
 *
 * Existen porque el resto de los tests del feed mockean el repositorio, así que el SQL
 * nunca se ejecuta en ellos: la primera versión de estas consultas pasaba diez tests
 * en verde y fallaba entera en Postgres. Esto no sustituye probarlo contra Postgres
 * —H2 no reproduce todo lo suyo— pero sí atrapa que la consulta deje de armarse, que
 * una columna no exista o que la proyección no entre en el record.
 */
@SpringBootTest
@ActiveProfiles("test")
class FeedQueryTest {

    private static final Instant DESDE_ARRIBA = Instant.parse("9999-12-31T23:59:59Z");

    /** Sin nadie bloqueado: la lista no puede ir vacía, así que lleva un id imposible. */
    private static final List<Long> NADIE = List.of(-1L);

    @Autowired private RatingRepository ratingRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private BurgerJointRepository burgerJointRepository;

    private User autor;
    private User otro;
    private BurgerJoint local;

    /**
     * Se limpian las reseñas, que es lo que el feed lee, y nada más.
     *
     * Las personas y el local se buscan antes de crearlos en vez de borrar esas tablas:
     * otras clases de test comparten esta base, y borrarles los usuarios de abajo les
     * rompe las suyas. Al terminar se vuelven a limpiar las reseñas por lo mismo, para
     * no dejar filas colgando de usuarios que otro test vaya a querer borrar.
     */
    @BeforeEach
    void setUp() {
        ratingRepository.deleteAll();

        autor = alguien("autorfeed");
        otro = alguien("otrofeed");
        local = unLocal();
    }

    @AfterEach
    void tearDown() {
        ratingRepository.deleteAll();
    }

    private User alguien(String username) {
        return userRepository.findByUsername(username).orElseGet(() -> userRepository.save(
            User.builder().username(username).email(username + "@example.com")
                .emailVerified(true).build()));
    }

    private BurgerJoint unLocal() {
        return burgerJointRepository.findByPlaceId("place-del-test-de-feed")
            .orElseGet(() -> burgerJointRepository.save(BurgerJoint.builder()
                .name("Un local").address("Una calle 123").area("Palermo")
                .placeId("place-del-test-de-feed").build()));
    }

    private Rating resenia(User de, int nota, Instant cuando) {
        return ratingRepository.save(Rating.builder()
            .user(de).burgerJoint(local).score(nota).comment("algo").createdAt(cuando).build());
    }

    private List<ItemDeFeedDto> desdeArriba() {
        return ratingRepository.feedDeTodos(DESDE_ARRIBA, Long.MAX_VALUE, NADIE, PageRequest.of(0, 10));
    }

    @Test
    void traeLaReseniaConQuienLaEscribioYDondeFue() {
        resenia(autor, 4, Instant.parse("2026-09-20T15:00:00Z"));

        assertThat(desdeArriba()).singleElement().satisfies(item -> {
            assertThat(item.autorUsername()).isEqualTo("autorfeed");
            assertThat(item.burgerJointName()).isEqualTo("Un local");
            assertThat(item.area()).isEqualTo("Palermo");
            assertThat(item.score()).isEqualTo(4);
        });
    }

    @Test
    void deLaMasNuevaALaMasVieja() {
        resenia(autor, 3, Instant.parse("2026-09-18T10:00:00Z"));
        resenia(otro, 5, Instant.parse("2026-09-20T10:00:00Z"));

        assertThat(desdeArriba()).extracting(ItemDeFeedDto::autorUsername)
            .containsExactly("otrofeed", "autorfeed");
    }

    /** Una reseña que nadie tocó no dice "editada". */
    @Test
    void sinEditarNoFiguraComoEditada() {
        resenia(autor, 4, Instant.parse("2026-09-20T15:00:00Z"));

        assertThat(desdeArriba()).singleElement()
            .extracting(ItemDeFeedDto::editada).isEqualTo(false);
    }

    /** Y una que sí, sí: es lo único para lo que existe updated_at. */
    @Test
    void despuesDeEditarlaFiguraComoEditada() {
        Rating guardada = resenia(autor, 4, Instant.parse("2026-09-20T15:00:00Z"));

        guardada.setScore(5);
        ratingRepository.saveAndFlush(guardada);

        assertThat(desdeArriba()).singleElement()
            .extracting(ItemDeFeedDto::editada).isEqualTo(true);
    }

    /**
     * El corte deja afuera la reseña a la que apunta, no la incluye de nuevo.
     *
     * Es lo que evita que la última de una página vuelva a salir como primera de la
     * siguiente.
     */
    @Test
    void elCorteSigueDespuesDeLaQueYaSeMostro() {
        Rating primera = resenia(autor, 5, Instant.parse("2026-09-20T10:00:00Z"));
        resenia(otro, 3, Instant.parse("2026-09-18T10:00:00Z"));

        List<ItemDeFeedDto> siguiente = ratingRepository.feedDeTodos(
            primera.getCreatedAt(), primera.getId(), NADIE, PageRequest.of(0, 10));

        assertThat(siguiente).extracting(ItemDeFeedDto::autorUsername).containsExactly("otrofeed");
    }

    /**
     * Con el mismo instante, el id decide.
     *
     * Dos personas pueden publicar en el mismo milisegundo: si el corte mirara solo la
     * fecha, una de las dos se perdería entre una página y la otra.
     */
    @Test
    void conElMismoInstanteElIdDesempata() {
        Instant mismoRato = Instant.parse("2026-09-20T10:00:00Z");
        Rating primera = resenia(autor, 5, mismoRato);
        Rating segunda = resenia(otro, 3, mismoRato);

        List<ItemDeFeedDto> siguiente = ratingRepository.feedDeTodos(
            mismoRato, Math.max(primera.getId(), segunda.getId()), NADIE, PageRequest.of(0, 10));

        assertThat(siguiente).extracting(ItemDeFeedDto::ratingId)
            .containsExactly(Math.min(primera.getId(), segunda.getId()));
    }

    @Test
    void siguiendoTraeSoloLasDeEsaGente() {
        resenia(autor, 4, Instant.parse("2026-09-20T10:00:00Z"));
        resenia(otro, 2, Instant.parse("2026-09-19T10:00:00Z"));

        List<ItemDeFeedDto> soloDeOtro = ratingRepository.feedDe(
            List.of(otro.getId()), DESDE_ARRIBA, Long.MAX_VALUE, NADIE, PageRequest.of(0, 10));

        assertThat(soloDeOtro).extracting(ItemDeFeedDto::autorUsername).containsExactly("otrofeed");
    }

    // ---- varias fotos por reseña (#185) ----

    private Rating conFotos(Rating resenia, String... fotos) {
        resenia.ponerFotos(List.of(fotos));
        return ratingRepository.saveAndFlush(resenia);
    }

    /** La consulta del feed trae la portada sola: las demás se agregan aparte. */
    @Test
    void elFeedTraeLaPortada() {
        conFotos(resenia(autor, 4, Instant.parse("2026-09-20T15:00:00Z")),
            "/api/rating-photos/a.jpg", "/api/rating-photos/b.jpg");

        assertThat(desdeArriba()).singleElement()
            .extracting(ItemDeFeedDto::fotosDeLaResenia)
            .isEqualTo(List.of("/api/rating-photos/a.jpg"));
    }

    @Test
    void lasFotosDeVariasReseniasVienenEnOrdenYPorResenia() {
        Rating una = conFotos(resenia(autor, 4, Instant.parse("2026-09-20T15:00:00Z")),
            "/api/rating-photos/c.jpg", "/api/rating-photos/a.jpg", "/api/rating-photos/b.jpg");
        Rating otra = conFotos(resenia(otro, 3, Instant.parse("2026-09-19T15:00:00Z")),
            "/api/rating-photos/z.jpg");

        var fotos = ratingRepository.fotosPorResenia(List.of(una.getId(), otra.getId()));

        assertThat(fotos.get(una.getId())).containsExactly(
            "/api/rating-photos/c.jpg", "/api/rating-photos/a.jpg", "/api/rating-photos/b.jpg");
        assertThat(fotos.get(otra.getId())).containsExactly("/api/rating-photos/z.jpg");
    }
}
