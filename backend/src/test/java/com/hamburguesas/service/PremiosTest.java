package com.hamburguesas.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/** Qué premios de la hamburguesa se pueden usar a cada nivel, y qué pasa al bajar (#313). */
class PremiosTest {

    @Test
    void sinPremiosNadaEstaBloqueado() {
        assertThat(Premios.bloqueado(null, 1)).isNull();
        assertThat(Premios.bloqueado("20131", 1)).isNull();
        assertThat(Premios.bloqueado("20133", 1)).isNull();
    }

    @ParameterizedTest
    @CsvSource({
        // receta, nivel, mensaje
        "20131100, 2, Todavía no desbloqueaste el huevo",
        "20131010, 4, Todavía no desbloqueaste el bacon",
        "20134, 9, Todavía no desbloqueaste la cuarta carne",
        "20131001, 14, Todavía no desbloqueaste el fondo de rayos",
    })
    void unPremioSinGanarSeRechazaConSuNombre(String receta, int nivel, String mensaje) {
        assertThat(Premios.bloqueado(receta, nivel)).isEqualTo(mensaje);
    }

    @ParameterizedTest
    @CsvSource({"20131100, 3", "20131010, 5", "20134, 10", "20131001, 15", "42234111, 20"})
    void alNivelQueLosDesbloqueaYaSePuedenUsar(String receta, int nivel) {
        assertThat(Premios.bloqueado(receta, nivel)).isNull();
    }

    @Test
    void alBajarSeRecortaLoQueYaNoAlcanza() {
        // Nivel 2: sin huevo, sin bacon, tres carnes y sin rayos; sin premios vuelve a cinco cifras.
        assertThat(Premios.recortar("20134111", 2)).isEqualTo("20133");
        // Nivel 4: el huevo se queda, el bacon se va.
        assertThat(Premios.recortar("20133110", 4)).isEqualTo("20133100");
        // Nivel 9: se pierde la cuarta carne pero el bacon queda.
        assertThat(Premios.recortar("20134010", 9)).isEqualTo("20133010");
        // Nivel 14: se pierden los rayos.
        assertThat(Premios.recortar("20134101", 14)).isEqualTo("20134100");
    }

    @Test
    void siTodoAlcanzaNoSeToca() {
        assertThat(Premios.recortar("42234111", 20)).isEqualTo("42234111");
        assertThat(Premios.recortar("20131", 1)).isEqualTo("20131");
        assertThat(Premios.recortar(null, 1)).isNull();
    }
}
