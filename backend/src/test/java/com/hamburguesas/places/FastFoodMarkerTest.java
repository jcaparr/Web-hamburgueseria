package com.hamburguesas.places;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cubre el reconocimiento de las cadenas por el nombre.
 *
 * El rubro que declara Google no sirve para esto: de las nueve sucursales de Mostaza
 * marca una como comida rápida y el resto como hamburguesería, y con Wendy's pasa lo
 * mismo. Clasifica desparejo dentro de una misma cadena.
 */
class FastFoodMarkerTest {

    private static final List<String> MARCAS = List.of("burgerking", "mcdonalds", "wendys", "mostaza");

    private boolean esCadena(String nombre) {
        return FastFoodMarker.esDeUnaCadena(nombre, MARCAS);
    }

    @Test
    void reconoceLasCuatroMarcas() {
        assertThat(esCadena("McDonald's")).isTrue();
        assertThat(esCadena("Burger King")).isTrue();
        assertThat(esCadena("Mostaza")).isTrue();
        assertThat(esCadena("Wendy's")).isTrue();
    }

    /** Las sucursales llevan el lugar pegado al nombre de la marca. */
    @Test
    void reconoceLasSucursales() {
        assertThat(esCadena("Burger King - Sucursal P.Italia")).isTrue();
        assertThat(esCadena("McDonald's Abasto Patio de Comidas")).isTrue();
        assertThat(esCadena("Wendy's Abasto")).isTrue();
    }

    /** En Google la misma cadena aparece escrita de varias formas. */
    @Test
    void noSeDejaEnganarPorLaPuntuacion() {
        assertThat(esCadena("Wendys")).isTrue();
        assertThat(esCadena("WENDY´S")).isTrue();
        assertThat(esCadena("Mc Donalds")).isTrue();
    }

    /**
     * Se pide el principio del nombre y no que la marca aparezca en cualquier lado.
     * Es más predecible: un local que menciona a otro no queda tapado por eso.
     */
    @Test
    void pideQueLaMarcaEsteAlPrincipio() {
        assertThat(esCadena("AutoMostaza")).isFalse();
        assertThat(esCadena("La esquina frente al McDonald's")).isFalse();
    }

    @Test
    void unaHamburgueseriaCualquieraNoEsCadena() {
        assertThat(esCadena("The Burger Company")).isFalse();
        assertThat(esCadena("La Birra Bar")).isFalse();
        assertThat(esCadena("Voraz")).isFalse();
    }

    @Test
    void sinMarcasConfiguradasNoHayCadenas() {
        assertThat(FastFoodMarker.esDeUnaCadena("McDonald's", List.of())).isFalse();
    }
}
