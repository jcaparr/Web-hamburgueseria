package com.hamburguesas.repository;

import com.hamburguesas.model.Follow;
import com.hamburguesas.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Las consultas de las listas de seguidores, ejecutadas contra una base (#183).
 *
 * Los tests del servicio mockean el repositorio: esto es lo que atrapa que la consulta
 * no se arme o que el orden y el filtro no sean los que se piden.
 */
@SpringBootTest
@ActiveProfiles("test")
class SeguidoresQueryTest {

    private static final List<Long> NADIE = List.of(-1L);

    @Autowired private FollowRepository followRepository;
    @Autowired private UserRepository userRepository;

    private User ella;
    private User ana;
    private User beto;
    private User carla;

    /** Como en FeedQueryTest: la base es compartida, así que se busca antes de crear. */
    @BeforeEach
    void setUp() {
        followRepository.deleteAll();
        ella = alguien("ellaseg");
        ana = alguien("anaseg");
        beto = alguien("betoseg");
        carla = alguien("carlaseg");
    }

    @AfterEach
    void tearDown() {
        followRepository.deleteAll();
    }

    private User alguien(String username) {
        return userRepository.findByUsername(username).orElseGet(() -> userRepository.save(
            User.builder().username(username).email(username + "@example.com").emailVerified(true).build()));
    }

    private void sigue(User quien, User a, String cuando) {
        followRepository.save(Follow.builder().follower(quien).followed(a).createdAt(Instant.parse(cuando)).build());
    }

    @Test
    void losSeguidoresVanDelMasRecienteAlMasViejo() {
        sigue(ana, ella, "2026-10-01T10:00:00Z");
        sigue(beto, ella, "2026-10-05T10:00:00Z");
        sigue(ella, carla, "2026-10-03T10:00:00Z");

        assertThat(followRepository.seguidoresDe(ella.getId(), NADIE))
            .extracting(User::getUsername).containsExactly("betoseg", "anaseg");
    }

    @Test
    void aQuienesSigueTraeLaOtraDireccion() {
        sigue(ella, carla, "2026-10-03T10:00:00Z");
        sigue(ana, ella, "2026-10-01T10:00:00Z");

        assertThat(followRepository.seguidosPor(ella.getId(), NADIE))
            .extracting(User::getUsername).containsExactly("carlaseg");
    }

    @Test
    void losOcultosNoSalen() {
        sigue(ana, ella, "2026-10-01T10:00:00Z");
        sigue(beto, ella, "2026-10-05T10:00:00Z");

        assertThat(followRepository.seguidoresDe(ella.getId(), List.of(beto.getId())))
            .extracting(User::getUsername).containsExactly("anaseg");
    }
}
