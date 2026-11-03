package com.hamburguesas.service;

import com.hamburguesas.dto.LocalDelMapaDto;
import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.model.FranjaHoraria;
import com.hamburguesas.model.Rating;
import com.hamburguesas.model.User;
import com.hamburguesas.repository.BurgerJointRepository;
import com.hamburguesas.repository.FranjaHorariaRepository;
import com.hamburguesas.repository.RatingRepository;
import com.hamburguesas.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.groups.Tuple.tuple;

/** Los locales del mapa (#395) contra la base: quién entra, con qué nota y con qué horario. */
@SpringBootTest
@ActiveProfiles("test")
class MapaDeLocalesTest {

    @Autowired private MapaDeLocales mapa;
    @Autowired private BurgerJointRepository locales;
    @Autowired private RatingRepository ratings;
    @Autowired private FranjaHorariaRepository franjas;
    @Autowired private UserRepository users;

    @BeforeEach
    @AfterEach
    void limpiar() {
        ratings.deleteAll();
        locales.findAll().stream()
            .filter(l -> l.getPlaceId() != null && l.getPlaceId().startsWith("mapa-"))
            .forEach(l -> {
                franjas.borrarLasDe(l.getId());
                locales.delete(l);
            });
    }

    private BurgerJoint local(String id, Double lat, Double lon, boolean cadena, String foto) {
        return locales.save(BurgerJoint.builder().name("Local " + id).address("Calle 1").area("Palermo")
            .slug("mapa-" + id).placeId("mapa-" + id).latitude(lat).longitude(lon).fastFood(cadena)
            .photoUrl(foto).build());
    }

    private User cuenta(String nombre) {
        return users.findByUsername(nombre).orElseGet(() -> users.save(
            User.builder().username(nombre).email(nombre + "@example.com").emailVerified(true).build()));
    }

    private LocalDelMapaDto delMapa(String id) {
        return mapa.locales().stream().filter(l -> ("mapa-" + id).equals(l.slug())).findFirst().orElse(null);
    }

    @Test
    void entranSoloLosQueTienenCoordenadasYNoSonCadenas() {
        local("ok", -34.6, -58.4, false, null);
        local("sin-coords", null, null, false, null);
        local("sin-lon", -34.6, null, false, null);
        local("cadena", -34.6, -58.4, true, null);

        assertThat(delMapa("ok")).isNotNull();
        assertThat(delMapa("sin-coords")).isNull();
        assertThat(delMapa("sin-lon")).isNull();
        assertThat(delMapa("cadena")).isNull();
    }

    @Test
    void laNotaYLasReseniasSeCuentanBien() {
        BurgerJoint conNota = local("nota", -34.6, -58.4, false, null);
        local("vacio", -34.6, -58.4, false, null);
        ratings.save(Rating.builder().user(cuenta("mapa_a")).burgerJoint(conNota).score(5).build());
        ratings.save(Rating.builder().user(cuenta("mapa_b")).burgerJoint(conNota).score(4).build());

        LocalDelMapaDto con = delMapa("nota");
        assertThat(con.ratingsCount()).isEqualTo(2);
        assertThat(con.averageScore()).isCloseTo(4.5, within(0.001));

        // Sin reseñas entra igual: el punto lleva solo la foto, sin nota.
        LocalDelMapaDto sin = delMapa("vacio");
        assertThat(sin.ratingsCount()).isZero();
        assertThat(sin.averageScore()).isNull();
    }

    @Test
    void elHorarioDeCadaLocalEsElSuyo() {
        BurgerJoint a = local("hor-a", -34.6, -58.4, false, null);
        BurgerJoint b = local("hor-b", -34.6, -58.4, false, null);
        local("hor-c", -34.6, -58.4, false, null);
        franjas.save(FranjaHoraria.builder().burgerJoint(a).dia(5).abre(1140).cierra(1500).build());
        franjas.save(FranjaHoraria.builder().burgerJoint(a).dia(1).abre(720).cierra(900).build());
        franjas.save(FranjaHoraria.builder().burgerJoint(b).dia(2).abre(600).cierra(700).build());

        // Ordenadas por día, y el cierre que pasa de 1440 llega tal cual.
        assertThat(delMapa("hor-a").franjas()).extracting("dia", "abre", "cierra")
            .containsExactly(tuple(1, 720, 900), tuple(5, 1140, 1500));
        assertThat(delMapa("hor-b").franjas()).hasSize(1);
        assertThat(delMapa("hor-c").franjas()).isEmpty();
    }

    @Test
    void laFotoSeServeEnMiniatura() {
        local("foto", -34.6, -58.4, false, "/api/place-photos/ChIJ1.jpg?v=abc123");

        assertThat(delMapa("foto").miniatura()).isEqualTo("/api/place-photos/mini/ChIJ1.jpg?v=abc123");
        assertThat(MapaDeLocales.miniaturaDe(null)).isNull();
        // Una foto que no es nuestra, o que ya es miniatura, no se toca.
        assertThat(MapaDeLocales.miniaturaDe("https://otro.com/x.jpg")).isEqualTo("https://otro.com/x.jpg");
        assertThat(MapaDeLocales.miniaturaDe("/api/place-photos/mini/x.jpg")).isEqualTo("/api/place-photos/mini/x.jpg");
    }
}
