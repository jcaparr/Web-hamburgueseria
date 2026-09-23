package com.hamburguesas.places;

import com.hamburguesas.model.BurgerJoint;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cubre cuándo dos fichas de Google son el mismo local.
 *
 * Google tiene dos fichas para algunos negocios, con identificadores distintos, y para
 * nosotros son dos locales: lo que evita repetidos es el identificador, y ahí son dos.
 * "24th Street Burger, Av. Triunvirato 4375" aparecía dos veces seguidas en la lista,
 * una con foto y la otra sin.
 */
class DuplicadosTest {

    /** Av. Triunvirato 4375, donde estaba el repetido de verdad. */
    private static final double LAT = -34.5765644;
    private static final double LON = -58.4822027;

    private BurgerJoint local(long id, String nombre, double lat, double lon, String foto) {
        return BurgerJoint.builder()
            .id(id).name(nombre).address("Una dirección").area("Villa Urquiza")
            .latitude(lat).longitude(lon).photoUrl(foto)
            .build();
    }

    @Test
    void elMismoNombreEnElMismoLugarEsElMismoLocal() {
        var una = local(469, "24th Street Burger", LAT, LON, "/api/place-photos/a.jpg");
        var otra = local(970, "24th Street Burger", LAT, LON, null);

        assertThat(Duplicados.sonElMismoLocal(una, otra)).isTrue();
    }

    /**
     * Dos locales distintos comparten dirección más seguido de lo que parece: las
     * cocinas que alquilan el mismo espacio son comunes, y en la base hay tres pares
     * así. Sin mirar el nombre nos llevaríamos uno de cada par.
     */
    @Test
    void dosLocalesDistintosEnLaMismaDireccionNoSonElMismo() {
        var uno = local(226, "Shark Burgers", LAT, LON, null);
        var otro = local(216, "Tempo The Burger shop", LAT, LON, null);

        assertThat(Duplicados.sonElMismoLocal(uno, otro)).isFalse();
    }

    /** Y una cadena tiene el mismo nombre en dos barrios sin ser el mismo local. */
    @Test
    void dosSucursalesDeLaMismaCadenaNoSonElMismo() {
        var palermo = local(926, "24th Street Burger", -34.5946734, -58.4290779, null);
        var urquiza = local(469, "24th Street Burger", LAT, LON, null);

        assertThat(Duplicados.sonElMismoLocal(palermo, urquiza)).isFalse();
    }

    /** El punto de una misma ficha puede estar corrido unos metros entre una y otra. */
    @Test
    void aceptaQueElPuntoEsteCorridoUnosMetros() {
        var una = local(1, "Voraz", LAT, LON, null);
        var conElPuntoCorrido = local(2, "Voraz", LAT + 0.0005, LON, null);

        assertThat(Duplicados.estanEnElMismoLugar(una, conElPuntoCorrido)).isTrue();
    }

    @Test
    void aMasDeCientoCincuentaMetrosYaNoEsElMismo() {
        var una = local(1, "Voraz", LAT, LON, null);
        var lejos = local(2, "Voraz", LAT + 0.002, LON, null);

        assertThat(Duplicados.estanEnElMismoLugar(una, lejos)).isFalse();
    }

    @Test
    void ignoraMayusculasAcentosYPuntuacion() {
        assertThat(Duplicados.mismoNombre("Voraz", "VORAZ!")).isTrue();
        assertThat(Duplicados.mismoNombre("Wendy's", "Wendys")).isTrue();
        assertThat(Duplicados.mismoNombre("Hamburguesería", "Hamburgueseria")).isTrue();
        assertThat(Duplicados.mismoNombre("Voraz", "Vorax")).isFalse();
    }

    /** Sin coordenadas no se puede afirmar que sea el mismo, así que no se toca. */
    @Test
    void sinCoordenadasNoSeArriesga() {
        var conPunto = local(1, "Voraz", LAT, LON, null);
        var sinPunto = BurgerJoint.builder()
            .id(2L).name("Voraz").address("Una dirección").area("Villa Urquiza").build();

        assertThat(Duplicados.sonElMismoLocal(conPunto, sinPunto)).isFalse();
    }

    /** Entre dos repetidos se conserva la que tiene foto, que es lo que se ve. */
    @Test
    void seQuedaLaQueTieneFoto() {
        var conFoto = local(970, "24th Street Burger", LAT, LON, "/api/place-photos/a.jpg");
        var sinFoto = local(469, "24th Street Burger", LAT, LON, null);

        assertThat(Duplicados.mejorDeLasDos(conFoto, sinFoto)).isSameAs(conFoto);
        assertThat(Duplicados.mejorDeLasDos(sinFoto, conFoto)).isSameAs(conFoto);
    }

    /** Si las dos están igual, la más vieja: puede estar enlazada desde algún lado. */
    @Test
    void anteIgualdadSeQuedaLaMasVieja() {
        var vieja = local(469, "24th Street Burger", LAT, LON, null);
        var nueva = local(970, "24th Street Burger", LAT, LON, null);

        assertThat(Duplicados.mejorDeLasDos(nueva, vieja)).isSameAs(vieja);
    }
}
