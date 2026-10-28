package com.hamburguesas.podio;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/** Qué semana cierra el podio según el día en que corre (#354). */
class CierreDePodioTest {

    private static final LocalDate LUNES_ANTERIOR = LocalDate.of(2026, 9, 7);

    @Test
    void unLunesCierraLaSemanaQueTerminoAyer() {
        assertThat(CierreDePodio.semanaQueTermino(LocalDate.of(2026, 9, 14))).isEqualTo(LUNES_ANTERIOR);
    }

    @Test
    void cualquierOtroDiaCierraLaSemanaPasadaYNuncaLaQueEstaCorriendo() {
        // De martes 15 a domingo 20 la semana en curso es la del lunes 14.
        for (int dia = 15; dia <= 20; dia++) {
            assertThat(CierreDePodio.semanaQueTermino(LocalDate.of(2026, 9, dia)))
                .as("hoy es %d de septiembre", dia)
                .isEqualTo(LocalDate.of(2026, 9, 7));
        }
    }
}
