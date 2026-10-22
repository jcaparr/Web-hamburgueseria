package com.hamburguesas.service;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/** La dirección linda de cada local (#305). */
class SlugsDeLocalesTest {

    @Test
    void sacaAcentosSignosYEspacios() {
        assertThat(SlugsDeLocales.slugDe("Átiko Burger & Grill")).isEqualTo("atiko-burger-y-grill");
        assertThat(SlugsDeLocales.slugDe("Wendy's  - Núñez ")).isEqualTo("wendys-nunez");
    }

    @Test
    void sinLetrasDaUnLocalGenerico() {
        assertThat(SlugsDeLocales.unicoPara("***", null, new HashSet<>())).isEqualTo("local");
    }

    @Test
    void siChocaSeLeSumaElBarrioYDespuesUnNumero() {
        Set<String> enUso = new HashSet<>();
        String a = SlugsDeLocales.unicoPara("Mostaza", "Palermo", enUso);
        enUso.add(a);
        String b = SlugsDeLocales.unicoPara("Mostaza", "Palermo", enUso);
        enUso.add(b);
        String c = SlugsDeLocales.unicoPara("Mostaza", "Palermo", enUso);

        assertThat(a).isEqualTo("mostaza");
        assertThat(b).isEqualTo("mostaza-palermo");
        assertThat(c).isEqualTo("mostaza-palermo-2");
    }

    @Test
    void unaPantallaDeLaAppNoSePuedeUsar() {
        assertThat(SlugsDeLocales.unicoPara("Ranking", "Villa Crespo", new HashSet<>()))
            .isEqualTo("ranking-villa-crespo");
    }

    /** Caddy no deja pasar a la app lo que la lista dice que es de la app: que no se desfasen. */
    @Test
    void elCaddyfileTieneLasMismasReservadas() throws IOException {
        String caddyfile = Files.readString(Path.of("..", "deploy", "Caddyfile"));
        Matcher m = Pattern.compile("not path_regexp reservada \\^/\\(([^)]*)\\)").matcher(caddyfile);
        assertThat(m.find()).isTrue();
        assertThat(Set.of(m.group(1).split("\\|"))).isEqualTo(SlugsDeLocales.RESERVADAS);
    }
}
