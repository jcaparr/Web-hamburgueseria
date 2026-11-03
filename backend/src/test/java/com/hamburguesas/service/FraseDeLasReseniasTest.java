package com.hamburguesas.service;

import com.hamburguesas.service.TemasDeLasResenias.Tema;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La oración del resumen (#392): que diga lo mismo que los chips y que se lea bien.
 *
 * Los temas se arman a mano con la nota ya contada, porque lo que se prueba acá es la
 * redacción; qué cuenta como mención se prueba en TemasDeLasReseniasTest.
 */
class FraseDeLasReseniasTest {

    private static Tema aFavor(String nombre) {
        return new Tema(nombre, 4, 4);
    }

    private static Tema enContra(String nombre) {
        return new Tema(nombre, 4, 0);
    }

    private static Tema dividido(String nombre) {
        return new Tema(nombre, 4, 2);
    }

    @Test
    void loQueGustaYLoQueNoEnUnaOracion() {
        assertThat(FraseDeLasResenias.de(List.of(
            aFavor("Las papas"), aFavor("La carne"), enContra("La espera"), dividido("El precio"))))
            .isEqualTo("Quienes escribieron destacan las papas y la carne, pero se quejan de la espera."
                + " El precio divide opiniones.");
    }

    @Test
    void soloLoQueGusta() {
        assertThat(FraseDeLasResenias.de(List.of(aFavor("Las papas"), aFavor("El pan"), aFavor("La salsa"))))
            .isEqualTo("Quienes escribieron destacan las papas, el pan y la salsa.");
    }

    /** "Del precio" y no "de el precio". */
    @Test
    void soloLoQueNoConLaContraccion() {
        assertThat(FraseDeLasResenias.de(List.of(enContra("El precio"), enContra("La espera"))))
            .isEqualTo("Quienes escribieron se quejan del precio y la espera.");
    }

    @Test
    void soloDivididosEnPlural() {
        assertThat(FraseDeLasResenias.de(List.of(dividido("El precio"), dividido("La atención"))))
            .isEqualTo("El precio y la atención dividen opiniones.");
    }

    /** Más de tres ya es una lista, y la lista está abajo en los chips. */
    @Test
    void nombraComoMuchoTresDeLosQueGustan() {
        assertThat(FraseDeLasResenias.de(List.of(
            aFavor("Las papas"), aFavor("La carne"), aFavor("El pan"), aFavor("La salsa"))))
            .isEqualTo("Quienes escribieron destacan las papas, la carne y el pan.");
    }

    /** Sin temas no hay frase: una genérica sería peor que nada. */
    @Test
    void sinTemasNoHayFrase() {
        assertThat(FraseDeLasResenias.de(List.of())).isNull();
    }
}
