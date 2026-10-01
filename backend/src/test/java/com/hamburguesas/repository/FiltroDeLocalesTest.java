package com.hamburguesas.repository;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Qué barrios llegan al filtro y cuáles se descartan antes.
 *
 * Importa porque la diferencia entre "no filtrar" y "filtrar por nada" no se ve: la
 * primera muestra todo y la segunda muestra una pantalla vacía, y las dos salen de una
 * dirección que a simple vista es igual.
 */
class FiltroDeLocalesTest {

    /** Varios barrios pasan tal cual: es el caso normal. */
    @Test
    void losBarriosElegidosPasan() {
        assertThat(FiltroDeLocales.losQueDicenAlgo(List.of("Palermo", "Belgrano")))
            .containsExactly("Palermo", "Belgrano");
    }

    /**
     * Un "?area=" suelto en la dirección llega como un elemento vacío.
     *
     * No es una lista vacía: si pasara, el filtro armaría un IN ('') y la pantalla
     * saldría sin resultados sin que se vea por qué.
     */
    @Test
    void unBarrioVacioNoFiltraPorNada() {
        assertThat(FiltroDeLocales.losQueDicenAlgo(List.of(""))).isEmpty();
        assertThat(FiltroDeLocales.losQueDicenAlgo(List.of("   "))).isEmpty();
    }

    /** Y los vacíos mezclados con los buenos se van, pero los buenos quedan. */
    @Test
    void losVaciosSeVanYLosOtrosQuedan() {
        assertThat(FiltroDeLocales.losQueDicenAlgo(List.of("Palermo", "", "Quilmes")))
            .containsExactly("Palermo", "Quilmes");
    }

    /** Sin el parámetro, Spring pasa null y eso quiere decir "no filtrar". */
    @Test
    void sinBarriosNoFiltra() {
        assertThat(FiltroDeLocales.losQueDicenAlgo(null)).isEmpty();
        assertThat(FiltroDeLocales.losQueDicenAlgo(List.of())).isEmpty();
    }

    /** Un nulo suelto adentro de la lista tampoco rompe. */
    @Test
    void unNuloEnLaListaNoRompe() {
        assertThat(FiltroDeLocales.losQueDicenAlgo(Arrays.asList("Palermo", null)))
            .containsExactly("Palermo");
    }
}
