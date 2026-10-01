package com.hamburguesas.texto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Las tres formas de normalizar, y sobre todo en qué se diferencian.
 *
 * Estaban copiadas en nueve lugares. Lo que importa fijar acá es la diferencia entre
 * ellas: son parecidas lo suficiente como para confundirlas y no lo suficiente como para
 * intercambiarlas, y usar una por otra cambia qué nombres se consideran iguales.
 */
class TextoTest {

    // ---- lo que hacen las tres ----

    @Test
    void lasTresSacanLosAcentosYPasanAMinuscula() {
        assertThat(Texto.paraComparar("Núñez")).isEqualTo("nunez");
        assertThat(Texto.soloLetrasYNumeros("Núñez")).isEqualTo("nunez");
        assertThat(Texto.sinAcentosEnMinuscula("Núñez")).isEqualTo("nunez");
    }

    @Test
    void lasTresDevuelvenVacioSiLlegaNulo() {
        assertThat(Texto.paraComparar(null)).isEmpty();
        assertThat(Texto.soloLetrasYNumeros(null)).isEmpty();
        assertThat(Texto.sinAcentosEnMinuscula(null)).isEmpty();
    }

    // ---- en qué se diferencian ----

    /** Para comparar nombres: los espacios de más se colapsan, pero las palabras quedan. */
    @Test
    void paraCompararColapsaLosEspaciosYConservaLasPalabras() {
        assertThat(Texto.paraComparar("  CHOPI'S   BURGER  ")).isEqualTo("chopi's burger");
    }

    /** Para reconocer una marca: se va todo lo que no sea letra o número. */
    @Test
    void soloLetrasYNumerosJuntaLasFormasDeEscribirUnaMarca() {
        assertThat(Texto.soloLetrasYNumeros("Wendy's"))
            .isEqualTo(Texto.soloLetrasYNumeros("WENDYS"))
            .isEqualTo("wendys");
        assertThat(Texto.soloLetrasYNumeros("Dean & Dennys")).isEqualTo("deandennys");
    }

    /** Para buscar palabras: los espacios y los signos quedan donde estaban. */
    @Test
    void sinAcentosEnMinusculaNoTocaNiEspaciosNiSignos() {
        assertThat(Texto.sinAcentosEnMinuscula("¡La  Atención!")).isEqualTo("¡la  atencion!");
    }
}
