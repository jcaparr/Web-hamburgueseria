package com.hamburguesas.places;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Cubre en qué zona cae un local fuera de la Ciudad.
 *
 * Adentro de la Ciudad manda el archivo de límites oficiales, que ya estaba probado.
 * Afuera la zona sale de la dirección, y las direcciones son reales: así las devuelve
 * Google para la Provincia.
 */
class ZonasTest {

    @Test
    void sacaLaLocalidadDeUnaDireccionDeLaProvincia() {
        assertThat(Zonas.localidadDe(
            "Brandsen 1204, B1814 Cañuelas, Provincia de Buenos Aires, Argentina"))
            .contains("Cañuelas");

        assertThat(Zonas.localidadDe(
            "Diag. 74 2017, B1900 La Plata, Provincia de Buenos Aires, Argentina"))
            .contains("La Plata");

        assertThat(Zonas.localidadDe(
            "John F. Kennedy 50, B1626 Belén de Escobar, Provincia de Buenos Aires, Argentina"))
            .contains("Belén de Escobar");
    }

    /** El código postal largo también: "B1814AZR Cañuelas" es la misma localidad. */
    @Test
    void sacaElCodigoPostalLargo() {
        assertThat(Zonas.localidadDe(
            "Lara 860, B1814AZR Cañuelas, Provincia de Buenos Aires, Argentina"))
            .contains("Cañuelas");
    }

    /**
     * Una dirección con una coma de más adentro de la calle no rompe.
     *
     * "Villa Borguesa, RP25 Local 3, B1626 Belén de Escobar, ..." tiene cuatro comas, y
     * la localidad sigue siendo el anteúltimo tramo antes de la provincia.
     */
    @Test
    void unaComaDeMasEnLaCalleNoConfundeLaLocalidad() {
        assertThat(Zonas.localidadDe(
            "Villa Borguesa, RP25 Local 3, B1626 Belén de Escobar, "
                + "Provincia de Buenos Aires, Argentina"))
            .contains("Belén de Escobar");
    }

    @Test
    void unaDireccionSinLaFormaEsperadaNoInventaLocalidad() {
        assertThat(Zonas.localidadDe("Una calle sin nada")).isEmpty();
        assertThat(Zonas.localidadDe("")).isEmpty();
        assertThat(Zonas.localidadDe(null)).isEmpty();
    }

    /**
     * Los bordes que se quisieron cubrir entran en 75 km, y lo que está más lejos no.
     *
     * Son las distancias reales desde el Obelisco, que es lo que decide hasta dónde se
     * busca.
     */
    @Test
    void losBordesQueSeQuisieronCubrirEntranEnElRadio() {
        assertThat(Zonas.kilometrosDesdeElCentro(-34.9215, -57.9545)).isLessThan(75);   // La Plata
        assertThat(Zonas.kilometrosDesdeElCentro(-35.0525, -58.7660)).isLessThan(75);   // Cañuelas
        assertThat(Zonas.kilometrosDesdeElCentro(-34.5582, -59.1247)).isLessThan(75);   // Luján
        assertThat(Zonas.kilometrosDesdeElCentro(-34.3267, -58.7593)).isLessThan(75);   // Belén de Escobar
        assertThat(Zonas.kilometrosDesdeElCentro(-35.0242, -58.4222)).isLessThan(75);   // San Vicente
    }

    /** Y Mar del Plata, que es de donde venían los 29 locales que hubo que borrar, no. */
    @Test
    void marDelPlataQuedaAfuera() {
        assertThat(Zonas.kilometrosDesdeElCentro(-37.9619, -57.5602)).isGreaterThan(75);
    }

    /** El centro mismo da cero, que es la forma de ver que la fórmula no está corrida. */
    @Test
    void elObeliscoEstaACeroKilometrosDeSiMismo() {
        assertThat(Zonas.kilometrosDesdeElCentro(-34.6037, -58.3816)).isCloseTo(0, within(0.01));
    }

    // ---- cómo queda escrita la zona ----

    /**
     * Cuando Google no manda localidad, el tramo que se lee es el código postal solo.
     *
     * Son direcciones reales, tal como vinieron. Sacarle el código postal dejaba "AAT"
     * de nombre de zona, y eso aparecía en el selector de Explorar como si fuera un
     * lugar al que se puede ir.
     */
    @Test
    void unaDireccionSinLocalidadNoInventaUnNombre() {
        assertThat(Zonas.localidadDe(
            "Av. Bartolomé Mitre 666, B1870 AAT, Cdad. Autónoma de Buenos Aires, Argentina"))
            .contains(Zonas.SIN_LOCALIDAD);

        assertThat(Zonas.localidadDe(
            "Julián Álvarez, B1744 1343, Provincia de Buenos Aires, Argentina"))
            .contains(Zonas.SIN_LOCALIDAD);
    }

    /**
     * Y no queda vacía, que es lo que la borraría.
     *
     * La limpieza borra los locales sin zona porque los lee como que están fuera del
     * radio. Un local que existe y está adentro no se puede perder porque Google escribió
     * la dirección corta.
     */
    @Test
    void unaDireccionSinLocalidadNoDejaAlLocalSinZona() {
        assertThat(Zonas.localidadDe(
            "Nicolás Avellaneda 54, B1804 EOB, Provincia de Buenos Aires, Argentina"))
            .isNotEmpty();
    }

    /** La misma localidad escrita de dos maneras tiene que dar una sola opción. */
    @Test
    void laMismaLocalidadSeEscribeSiempreIgual() {
        assertThat(Zonas.localidadDe("Calle 1, B1804 ezeiza, Provincia de Buenos Aires, Argentina"))
            .isEqualTo(Zonas.localidadDe(
                "Calle 2, B1804 Ezeiza, Provincia de Buenos Aires, Argentina"));
    }

    /** Las abreviadas también: son las que aparecieron de verdad en la base. */
    @Test
    void lasAbreviadasSeEscribenCompletas() {
        assertThat(Zonas.localidadDe(
            "Calle 1, B1846 Almte. Brown, Provincia de Buenos Aires, Argentina"))
            .contains("Almirante Brown");

        assertThat(Zonas.localidadDe(
            "Calle 1, B1748 3 de Febrero, Provincia de Buenos Aires, Argentina"))
            .contains("Tres de Febrero");
    }

    /** "Lomas de Zamora - GBA Sur" es Lomas de Zamora. */
    @Test
    void sacaElSufijoDeRegion() {
        assertThat(Zonas.localidadDe(
            "Calle 1, B1832 Lomas de Zamora - GBA Sur, Provincia de Buenos Aires, Argentina"))
            .contains("Lomas de Zamora");
    }

    /** Las partículas quedan en minúscula: "Lomas De Zamora" no lo escribe nadie. */
    @Test
    void lasParticulasNoLlevanMayuscula() {
        assertThat(Zonas.localidadDe(
            "Calle 1, B1832 lomas de zamora, Provincia de Buenos Aires, Argentina"))
            .contains("Lomas de Zamora");
    }

    /** Y lo que ya venía bien escrito no se toca. */
    @Test
    void loQueYaEstabaBienQuedaIgual() {
        assertThat(Zonas.localidadDe(
            "Diag. 74 2017, B1900 La Plata, Provincia de Buenos Aires, Argentina"))
            .contains("La Plata");

        assertThat(Zonas.localidadDe(
            "John F. Kennedy 50, B1626 Belén de Escobar, Provincia de Buenos Aires, Argentina"))
            .contains("Belén de Escobar");
    }
}
