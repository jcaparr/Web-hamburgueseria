package com.hamburguesas.places;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.hamburguesas.places.Veredicto.Prueba;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cubre qué local vende hamburguesas y cuál no.
 *
 * Los casos son reales y salieron de mirar la base: son los que el filtro anterior
 * clasificaba mal, en las dos direcciones. Los rubros y los resúmenes están copiados de
 * lo que devuelve Google, no inventados.
 */
class VendeHamburguesasTest {

    private static final Set<String> RUBROS_DE_OTRA_COSA = Set.of(
        "pizza_restaurant", "bakery", "barbecue_restaurant", "steak_house",
        "ice_cream_shop", "grocery_store", "convenience_store", "butcher_shop");

    private static final Set<String> SIN_CADENAS = Set.of();

    private Veredicto evaluar(String nombre, Set<String> rubros, String resumen) {
        return VendeHamburguesas.evaluar(nombre, rubros, resumen, RUBROS_DE_OTRA_COSA, SIN_CADENAS);
    }

    // ---- los que quedaban afuera y sí lo son ----

    /**
     * El caso que motivó todo esto.
     *
     * "Austin's Diner & Grill" no tiene hamburger_restaurant entre sus rubros y el
     * nombre no dice hamburguesa, así que las tres pruebas baratas fallan. Lo salva el
     * resumen de reseñas, que es lo que la gente que fue dice que comió.
     */
    @Test
    void entraElDinerQueNoDiceHamburguesaEnNingunLado() {
        Veredicto veredicto = evaluar(
            "Austin's Diner & Grill",
            Set.of("american_restaurant", "restaurant", "food"),
            "Diners like this restaurant's delicious grilled and smash burgers, noting "
                + "the high quality of ingredients and generous portions.");

        assertThat(veredicto.vendeHamburguesas()).isTrue();
        assertThat(veredicto.prueba()).isEqualTo(Prueba.LO_DICEN_LAS_RESENIAS);
    }

    /**
     * "La Birra Bar Colegiales" figura como restaurante, y es de las hamburguesas más
     * conocidas de Buenos Aires. El rubro principal no la salva; la lista completa sí.
     */
    @Test
    void entraLaQueGoogleClasificaComoRestauranteEnElRubroPrincipal() {
        Veredicto veredicto = evaluar(
            "La Birra Bar Colegiales",
            Set.of("restaurant", "hamburger_restaurant", "american_restaurant"),
            null);

        assertThat(veredicto.vendeHamburguesas()).isTrue();
        assertThat(veredicto.prueba()).isEqualTo(Prueba.RUBRO_DE_GOOGLE);
    }

    /**
     * "Panchos Y Hamburguesas" es un almacén que además vende hamburguesas. Tener el
     * rubro de hamburguesas lo salva del filtro de "se dedica a otra cosa".
     */
    @Test
    void entraElAlmacenQueTambienEsHamburgueseria() {
        Veredicto veredicto = evaluar(
            "Panchos Y Hamburguesas",
            Set.of("hamburger_restaurant", "grocery_store", "food_store", "store"),
            null);

        assertThat(veredicto.vendeHamburguesas()).isTrue();
    }

    // ---- los que quedaban adentro y no lo son ----

    /**
     * Un kiosco sin nada que lo relacione con hamburguesas queda afuera por su rubro,
     * aunque el nombre pueda mencionarlas.
     */
    @Test
    void noEntraElKioscoQueMencionaHamburguesasEnElNombre() {
        Veredicto veredicto = evaluar(
            "Kiosco Hamburguesas Mendoza",
            Set.of("convenience_store", "store"),
            null);

        assertThat(veredicto.vendeHamburguesas()).isFalse();
        assertThat(veredicto.prueba()).isEqualTo(Prueba.SE_DEDICA_A_OTRA_COSA);
    }

    /**
     * Una pizzería que se llama "Pizza, Birra & Burger" entraba por la regla del nombre,
     * que es la puerta más ancha. Su rubro la frena.
     */
    @Test
    void noEntraLaPizzeriaQueSeLlamaBurger() {
        Veredicto veredicto = evaluar(
            "La Estación - Pizza, Birra & Burger",
            Set.of("pizza_restaurant", "restaurant"),
            null);

        assertThat(veredicto.vendeHamburguesas()).isFalse();
        assertThat(veredicto.prueba()).isEqualTo(Prueba.SE_DEDICA_A_OTRA_COSA);
    }

    /**
     * Si el resumen habla de lo que vende el local y no son hamburguesas, eso pesa más
     * que no haber encontrado ninguna prueba: es lo que dice la gente que fue.
     */
    @Test
    void noEntraElLocalCuyasReseniasHablanDeOtraCosa() {
        Veredicto veredicto = evaluar(
            "Lo de Beto",
            Set.of("restaurant", "food"),
            "Diners praise the generous milanesas and the homemade pasta, and mention "
                + "the friendly service.");

        assertThat(veredicto.vendeHamburguesas()).isFalse();
        assertThat(veredicto.prueba()).isEqualTo(Prueba.LAS_RESENIAS_HABLAN_DE_OTRA_COSA);
    }

    /**
     * Sin ninguna prueba no entra.
     *
     * Es la diferencia con el filtro anterior, que ante la duda dejaba entrar por el
     * nombre: la lista se llenaba de bares y parrillas que mencionan hamburguesas.
     */
    @Test
    void sinNingunaPruebaNoEntra() {
        Veredicto veredicto = evaluar("Restó", Set.of("restaurant", "food"), null);

        assertThat(veredicto.vendeHamburguesas()).isFalse();
        assertThat(veredicto.prueba()).isEqualTo(Prueba.SIN_PRUEBAS);
    }

    /**
     * La ausencia de resumen no prueba nada: solo seis de cada diez locales tienen uno.
     * Un local con el rubro correcto entra igual sin resumen.
     */
    @Test
    void noTenerResumenNoDescalifica() {
        Veredicto veredicto = evaluar("Voraz", Set.of("hamburger_restaurant"), null);

        assertThat(veredicto.vendeHamburguesas()).isTrue();
    }

    // ---- lo que ya andaba y tiene que seguir andando ----

    @Test
    void elNombreQueLoDiceAlcanza() {
        assertThat(evaluar("The Burger Company", Set.of("restaurant"), null).vendeHamburguesas())
            .isTrue();
        assertThat(evaluar("Hamburguesas Venezolanas", Set.of("restaurant"), null).vendeHamburguesas())
            .isTrue();
        assertThat(evaluar("Mr Smash", Set.of("restaurant"), null).vendeHamburguesas())
            .isTrue();
    }

    @Test
    void reconoceLaSucursalDeUnaCadenaPorElPrincipioDelNombre() {
        Set<String> cadenas = Set.of("la birra bar");

        Veredicto veredicto = VendeHamburguesas.evaluar(
            "La Birra Bar Colegiales", Set.of("restaurant"), null, RUBROS_DE_OTRA_COSA, cadenas);

        assertThat(veredicto.prueba()).isEqualTo(Prueba.SUCURSAL_DE_UNA_CADENA);
    }

    @Test
    void unNombreQueApenasMencionaLaMarcaNoEsSucursal() {
        Set<String> cadenas = Set.of("la birra bar");

        Veredicto veredicto = VendeHamburguesas.evaluar(
            "La esquina frente a La Birra Bar", Set.of("restaurant"), null,
            RUBROS_DE_OTRA_COSA, cadenas);

        assertThat(veredicto.vendeHamburguesas()).isFalse();
    }

    /** Los acentos y las mayúsculas no tienen que cambiar el resultado. */
    @Test
    void noSeDejaEnganarPorAcentosNiMayusculas() {
        assertThat(evaluar("HAMBURGUESERÍA EL ÑATO", Set.of("restaurant"), null)
            .vendeHamburguesas()).isTrue();
    }
}
