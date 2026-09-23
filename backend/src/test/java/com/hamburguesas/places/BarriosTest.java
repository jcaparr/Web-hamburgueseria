package com.hamburguesas.places;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cubre que el barrio salga de las coordenadas y no de la búsqueda.
 *
 * Los puntos de abajo son direcciones reales que estaban mal en la base: 143 de los
 * 438 locales mostraban el barrio de la búsqueda que los había encontrado, y Google
 * se pasa de largo del barrio que se le pide.
 */
class BarriosTest {

    private final Barrios barrios = new Barrios();

    @Test
    void ubicaUnLocalEnElBarrioDeSuDireccion() {
        // Honduras 4733: la base lo tenía en Villa Real, a doce kilómetros.
        assertThat(barrios.barrioDe(-34.5900, -58.4270)).contains("Palermo");
    }

    @Test
    void conoceLosBarriosDelCentro() {
        // El Obelisco.
        assertThat(barrios.barrioDe(-34.6037, -58.3816)).contains("San Nicolás");
    }

    /**
     * La búsqueda es texto libre, así que "hamburguesería en San Nicolás" trajo locales
     * de San Nicolás de los Arroyos, "Versalles" uno de Colombia y "San Cristóbal" uno
     * de México. Los 29 que había en la base no tenían nada que ver con la Ciudad.
     */
    @Test
    void dejaAfueraLoQueNoEstaEnLaCiudad() {
        // Av. Constitución 4205, Mar del Plata.
        assertThat(barrios.barrioDe(-37.9694857, -57.5455363)).isEmpty();
        // Bartolomé Mitre 452, San Nicolás de los Arroyos.
        assertThat(barrios.barrioDe(-33.3354774, -60.2235931)).isEmpty();
        // Real de Guadalupe 47, San Cristóbal de las Casas, México.
        assertThat(barrios.barrioDe(16.7374567, -92.6340131)).isEmpty();
    }

    /**
     * El conurbano empieza del otro lado de una avenida, así que hay locales a cien
     * metros del límite que igual quedan afuera: la app es de la Ciudad.
     */
    @Test
    void dejaAfueraLoQueEstaApenasDelOtroLadoDelLimite() {
        // Estados Unidos 97 bis, Villa Martelli: a 92 metros de Saavedra.
        assertThat(barrios.barrioDe(-34.5488024, -58.5011041)).isEmpty();
    }

    /**
     * El dataset oficial marca los espejos de agua como agujeros del polígono, y un
     * local sobre los diques cae justo adentro de uno. Si se respetaran los agujeros,
     * "Isla Puerto Madero" se quedaría sin barrio y lo borraríamos por estar "fuera de
     * la Ciudad".
     */
    @Test
    void ubicaUnLocalSobreLosDiques() {
        // Pierina Dealessi 701.
        assertThat(barrios.barrioDe(-34.6056446, -58.3647742)).contains("Puerto Madero");
    }

    @Test
    void sinCoordenadasNoHayBarrio() {
        assertThat(barrios.barrioDe(null, -58.43)).isEmpty();
        assertThat(barrios.barrioDe(-34.58, null)).isEmpty();
    }

    /** Los 48 de la ley 2.650, con los nombres escritos como los muestra la app. */
    @Test
    void estanLosCuarentaYOchoBarrios() {
        // Av. Triunvirato 4710.
        assertThat(barrios.barrioDe(-34.5741943, -58.4866837)).contains("Villa Urquiza");
        // Av. de los Constituyentes 4234.
        assertThat(barrios.barrioDe(-34.5841164, -58.4898419)).contains("Villa Pueyrredón");
        // Montiel 1694.
        assertThat(barrios.barrioDe(-34.6567565, -58.5183596)).contains("Mataderos");
        // Av. Almirante Brown al 900.
        assertThat(barrios.barrioDe(-34.6345, -58.3634)).contains("La Boca");
    }
}
