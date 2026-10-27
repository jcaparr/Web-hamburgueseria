package com.hamburguesas.service;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La tabla de niveles y las reglas de XP (#310).
 *
 * Si la tabla cambia, cambia también {@code frontend/src/utils/niveles.ts}: este test
 * existe para que no cambie sola.
 */
class NivelesTest {

    private static final String DE_VERDAD =
        "Muy buena la carne, el pan estaba tostado justo y la salsa me encantó bastante; las papas también salieron crocantes";

    @Test
    void laTablaDeUmbralesEsEstaYNoOtra() {
        assertThat(Niveles.UMBRALES).containsExactly(
            0, 100, 200, 400, 600, 800, 1000, 1100, 1300, 1500,
            2000, 2500, 3000, 3500, 4000, 4500, 5000, 5500, 6000, 6500);
        assertThat(Niveles.NIVEL_MAXIMO).isEqualTo(20);
    }

    @Test
    void elNivelCambiaJustoEnElUmbral() {
        assertThat(Niveles.nivelDe(0)).isEqualTo(1);
        assertThat(Niveles.nivelDe(99)).isEqualTo(1);
        assertThat(Niveles.nivelDe(100)).isEqualTo(2);
        assertThat(Niveles.nivelDe(399)).isEqualTo(3);
        assertThat(Niveles.nivelDe(400)).isEqualTo(4);
        assertThat(Niveles.nivelDe(1999)).isEqualTo(10);
        assertThat(Niveles.nivelDe(2000)).isEqualTo(11);
        assertThat(Niveles.nivelDe(6499)).isEqualTo(19);
        assertThat(Niveles.nivelDe(6500)).isEqualTo(20);
    }

    @Test
    void despuesDelVeinteLaXpSigueSumandoPeroElNivelNoPasa() {
        assertThat(Niveles.nivelDe(50_000)).isEqualTo(20);
    }

    @Test
    void conFotoSolaDaSetentaYConComentarioDeVerdadCien() {
        assertThat(Niveles.xpDe(null)).isEqualTo(70);
        assertThat(Niveles.xpDe("")).isEqualTo(70);
        assertThat(Niveles.xpDe("muy buena")).isEqualTo(70);
        assertThat(Niveles.xpDe(DE_VERDAD)).isEqualTo(100);
    }

    @Test
    void elComentarioNecesitaCincoPalabras() {
        // Cuatro palabras: le falta una, por largas que sean.
        assertThat(Niveles.xpDe("muy buena la carne")).isEqualTo(70);
        assertThat(Niveles.xpDe(("a".repeat(200) + " ").repeat(4))).isEqualTo(70);
        // Muchas letras pegadas son una sola palabra.
        assertThat(Niveles.xpDe("a".repeat(200))).isEqualTo(70);
    }

    @Test
    void cincoPalabrasJustasYaCuentanSinMinimoDeLetras() {
        assertThat(Niveles.xpDe("muy buena la carne eh")).isEqualTo(100);
        assertThat(Niveles.xpDe("a b c d e")).isEqualTo(100);
        // Los espacios de los costados y los dobles no suman palabras.
        assertThat(Niveles.xpDe("  muy   buena  la   carne  ")).isEqualTo(70);
    }

    @Test
    void losSignosYLosNumerosNoSonPalabras() {
        assertThat(Niveles.xpDe("! ! ! ! ! ! !")).isEqualTo(70);
        assertThat(Niveles.xpDe("1 2 3 4 5 6 7 8 9 10")).isEqualTo(70);
        // Una palabra con letras y signos sí cuenta.
        assertThat(Niveles.xpDe("¡ja! ¡ja! ¡ja! ¡ja! ¡ja!")).isEqualTo(100);
    }

    @Test
    void sinReseniasNoHayXpNiBienvenida() {
        assertThat(Niveles.xpTotal(List.of())).isZero();
    }

    @Test
    void laBienvenidaSeSumaUnaSolaVez() {
        assertThat(Niveles.xpTotal(List.of(DE_VERDAD))).isEqualTo(130);
        assertThat(Niveles.xpTotal(Arrays.asList("hola", null, DE_VERDAD))).isEqualTo(70 + 70 + 100 + 30);
    }
}
