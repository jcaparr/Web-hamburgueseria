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

    /**
     * De otras provincias también (#222), aunque la dirección no traiga la provincia: una
     * capital puede venir como "X5000 Córdoba, Argentina", y el anteúltimo tramo antes de
     * la provincia sería la calle.
     */
    @Test
    void sacaLaLocalidadDeOtrasProvincias() {
        assertThat(Zonas.localidadDe("Av. Rafael Núñez 4624, X5009 Córdoba, Argentina"))
            .contains("Córdoba");
        assertThat(Zonas.localidadDe(
            "Bartolomé Mitre 452, S2900 San Nicolás de los Arroyos, Santa Fe, Argentina"))
            .contains("San Nicolás de los Arroyos");
        assertThat(Zonas.localidadDe(
            "Av. Constitución 4205, B7600 Mar del Plata, Provincia de Buenos Aires, Argentina"))
            .contains("Mar del Plata");
    }

    /** El país es el último tramo: "Versalles" trajo uno de Colombia. */
    @Test
    void reconoceLasDireccionesDeArgentina() {
        assertThat(Zonas.esDeArgentina("Av. Rafael Núñez 4624, X5009 Córdoba, Argentina")).isTrue();
        assertThat(Zonas.esDeArgentina("Cra. 8 #4-12, Floridablanca, Santander, Colombia")).isFalse();
        assertThat(Zonas.esDeArgentina("Una dirección")).isFalse();
        assertThat(Zonas.esDeArgentina(null)).isFalse();
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
// ---- el código postal pegado al nombre ----

    /**
     * Cuando Google manda dos códigos postales, el segundo queda pegado a la localidad.
     *
     * Es la dirección real de un local de San Miguel: el código largo, el viejo de cuatro
     * números y el nombre, sin un espacio que los separe. Sacar el primero dejaba
     * "1390San Miguel", y así aparecía en el selector de Explorar, al lado de San Miguel.
     */
    @Test
    void elCodigoPostalPegadoAlNombreNoEsParteDeLaZona() {
        assertThat(Zonas.localidadDe("Av. Pte. J. D. Perón, B1663EDH 1390San Miguel, "
            + "Provincia de Buenos Aires, Argentina"))
            .contains("San Miguel");
    }

    /** La otra forma del mismo problema: lo pegado son las tres letras del final. */
    @Test
    void laColaDelCodigoPegadaAlNombreTampoco() {
        assertThat(Zonas.localidadDe(
            "Calle 1, B1663 GRGSan Miguel, Provincia de Buenos Aires, Argentina"))
            .contains("San Miguel");
    }

    /** Y el código entero pegado, sin ningún espacio en el tramo. */
    @Test
    void elCodigoEnteroPegadoAlNombreTampoco() {
        assertThat(Zonas.localidadDe(
            "Calle 1, B1663EDH1390San Miguel, Provincia de Buenos Aires, Argentina"))
            .contains("San Miguel");
    }

    /**
     * Que es lo que importa: termina en la misma zona que el local de al lado.
     *
     * Dos nombres distintos para el mismo lugar son dos opciones en el selector, y el que
     * elige una no ve los locales de la otra.
     */
    @Test
    void elLocalConElCodigoPegadoCaeEnLaMismaZonaQueElVecino() {
        assertThat(Zonas.localidadDe("Av. Pte. J. D. Perón, B1663EDH 1390San Miguel, "
            + "Provincia de Buenos Aires, Argentina"))
            .isEqualTo(Zonas.localidadDe(
                "Av. Mitre 100, B1663 San Miguel, Provincia de Buenos Aires, Argentina"));
    }

    /**
     * Y no parte al medio a las que empiezan con una palabra corta.
     *
     * Es el riesgo de la regla: "La Plata" también arranca con mayúsculas seguidas de un
     * nombre. Lo que las salva es el espacio, así que estas son las que lo prueban.
     */
    @Test
    void noLeCortaElPrincipioALasLocalidadesDeVerdad() {
        assertThat(Zonas.localidadDe(
            "Calle 1, B1900 La Plata, Provincia de Buenos Aires, Argentina"))
            .contains("La Plata");

        assertThat(Zonas.localidadDe(
            "Calle 1, B1684 El Palomar, Provincia de Buenos Aires, Argentina"))
            .contains("El Palomar");

        assertThat(Zonas.localidadDe(
            "Calle 1, B1663 San Miguel, Provincia de Buenos Aires, Argentina"))
            .contains("San Miguel");

        assertThat(Zonas.localidadDe(
            "Calle 1, B1611 Don Torcuato, Provincia de Buenos Aires, Argentina"))
            .contains("Don Torcuato");
    }

    /** Lo que ya se reconocía entero sigue yendo a la zona genérica, no a un nombre raro. */
    @Test
    void elRestoDelCodigoSoloSigueSinInventarUnNombre() {
        assertThat(Zonas.localidadDe(
            "Calle 1, B1870 AAT, Provincia de Buenos Aires, Argentina"))
            .contains(Zonas.SIN_LOCALIDAD);
    }

    // ---- las abreviadas que aparecieron después ----

    /**
     * "I.casanova" e "Isidro Casanova" son el mismo lugar, y estaban las dos en la base.
     *
     * Era una zona de un local sola, al lado de la de seis: quien la elige ve uno y se
     * pierde los otros seis.
     */
    @Test
    void isidroCasanovaAbreviadaEsLaMisma() {
        assertThat(Zonas.localidadDe(
            "Voissin 106, B1765 I.casanova, Provincia de Buenos Aires, Argentina"))
            .isEqualTo(Zonas.localidadDe(
                "Calle 1, B1765 Isidro Casanova, Provincia de Buenos Aires, Argentina"));
    }

    /** Las otras dos que aparecieron con la misma forma. */
    @Test
    void lasOtrasAbreviadasTambienSeEscribenCompletas() {
        assertThat(Zonas.localidadDe(
            "San Martín 556, B1635 Pres. Derqui, Provincia de Buenos Aires, Argentina"))
            .contains("Presidente Derqui");

        assertThat(Zonas.localidadDe("Temperley Buenos Aires AR, Av pasco 3957, B1834 San Jose, "
            + "Provincia de Buenos Aires, Argentina"))
            .contains("San José");
    }
}
