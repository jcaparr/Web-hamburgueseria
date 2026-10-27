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

    /** 79 letras: una menos que lo que pide el comentario. */
    private static final String CASI = "aaaaaaaa ".repeat(8) + "aaaaaaa";
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
    void elComentarioNecesitaLasOchentaLetrasYLasDiezPalabrasLasDosCosas() {
        // 79 letras en 8 palabras de 9: le falta una letra y también palabras.
        assertThat(Niveles.xpDe(CASI)).isEqualTo(70);
        // Muchas letras pero pocas palabras.
        assertThat(Niveles.xpDe("a".repeat(200))).isEqualTo(70);
        // Muchas palabras pero pocas letras.
        assertThat(Niveles.xpDe("a b c d e f g h i j k l")).isEqualTo(70);
    }

    @Test
    void ochentaLetrasYDiezPalabrasJustasYaCuentan() {
        String justo = "aaaaaaaa ".repeat(9) + "aaaaaaaa";
        assertThat(justo.chars().filter(Character::isLetter).count()).isEqualTo(80);
        assertThat(Niveles.xpDe(justo)).isEqualTo(100);
    }

    @Test
    void losSignosYLosNumerosNoSonLetras() {
        String relleno = "!!!!!!!!!! ".repeat(20) + "1234567890 ".repeat(5);
        assertThat(Niveles.xpDe(relleno)).isEqualTo(70);
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
