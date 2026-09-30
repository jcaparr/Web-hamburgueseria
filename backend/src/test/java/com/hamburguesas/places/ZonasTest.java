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
}
