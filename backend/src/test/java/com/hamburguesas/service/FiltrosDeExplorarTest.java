package com.hamburguesas.service;

import com.hamburguesas.dto.BurgerJointDto;
import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.repository.BurgerJointRepository;
import com.hamburguesas.repository.OrdenDeLocales;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cubre los filtros de Explorar: el nombre y el barrio, y que las cadenas no salgan.
 *
 * Las cadenas eran un filtro más, que se podía apagar. Desde #206 no salen nunca: son
 * 295 sucursales de siete marcas, y se ven buscándolas por nombre en /api/cadenas, con
 * una tarjeta por cadena.
 *
 * Lo que más importa acá es que el nombre y el barrio se combinen. Eran consultas
 * separadas, una por combinación, y sumar un filtro más multiplicaba la cuenta.
 *
 * Contra una base de verdad, porque lo que se prueba son las consultas.
 */
@SpringBootTest
@ActiveProfiles("test")
class FiltrosDeExplorarTest {

    @Autowired private BurgerJointService service;
    @Autowired private BurgerJointRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        guardar("McDonald's", "Palermo", true);
        guardar("Burger King", "Palermo", true);
        guardar("The Burger Company", "Palermo", false);
        guardar("Voraz", "Boedo", false);
        guardar("Burger de Boedo", "Boedo", false);
    }

    private void guardar(String nombre, String barrio, boolean cadena) {
        repository.save(BurgerJoint.builder()
            .name(nombre).address("Una dirección").area(barrio)
            .fastFood(cadena)
            .build());
    }

    private List<String> nombres(String query, List<String> barrios) {
        return service.search(query, barrios, OrdenDeLocales.RELEVANTES, null, PageRequest.of(0, 20))
            .map(BurgerJointDto::name)
            .toList();
    }

    // ---- las cadenas ----

    @Test
    void lasCadenasNoSalenNunca() {
        assertThat(nombres(null, (List<String>) null))
            .containsExactlyInAnyOrder("The Burger Company", "Voraz", "Burger de Boedo");
    }

    /** Ni buscándolas por nombre: eso lo contesta /api/cadenas, con una tarjeta por cadena. */
    @Test
    void niBuscandolasPorNombre() {
        assertThat(nombres("mcdonald", (List<String>) null)).isEmpty();
        assertThat(nombres("burger", List.of("Palermo"))).containsExactly("The Burger Company");
    }

    /** El total de la paginación también tiene que bajar, o la lista queda con huecos. */
    @Test
    void elTotalReflejaElFiltro() {
        assertThat(service.search(null, (List<String>) null, OrdenDeLocales.RELEVANTES, null, PageRequest.of(0, 20)).getTotalElements())
            .isEqualTo(3);
        assertThat(service.search(null, List.of("Boedo"), OrdenDeLocales.RELEVANTES, null, PageRequest.of(0, 20)).getTotalElements())
            .isEqualTo(2);
    }

    // ---- el barrio ----

    @Test
    void elBarrioDejaSoloLosDeEseBarrio() {
        assertThat(nombres(null, List.of("Boedo")))
            .containsExactlyInAnyOrder("Voraz", "Burger de Boedo");
    }

    @Test
    void sinBarrioSeVeLaCiudadEntera() {
        assertThat(nombres(null, (List<String>) null)).hasSize(3);
        assertThat(nombres(null, List.of(""))).hasSize(3);
    }

    @Test
    void unBarrioSinLocalesNoDevuelveNada() {
        assertThat(nombres(null, List.of("Mataderos"))).isEmpty();
    }

    // ---- los dos juntos ----

    /**
     * El nombre y el barrio se aplican a la vez, que es lo que hace un listado usable:
     * "una hamburguesería que se llame burger, en Boedo".
     */
    @Test
    void elNombreYElBarrioSeCombinan() {
        assertThat(nombres("burger", (List<String>) null))
            .containsExactlyInAnyOrder("The Burger Company", "Burger de Boedo");
        assertThat(nombres("burger", List.of("Boedo")))
            .containsExactly("Burger de Boedo");
    }

    /**
     * El % y el _ son comodines del LIKE, y el buscador los deja escribir.
     *
     * Sin escaparlos, buscar "%" devolvía el listado entero, que es exactamente lo
     * contrario de lo que esperaría alguien que escribió un carácter.
     */
    @Test
    void losComodinesDelBuscadorSonTextoYNoComodines() {
        assertThat(nombres("%", (List<String>) null)).isEmpty();
        assertThat(nombres("_", (List<String>) null)).isEmpty();
    }

    /**
     * Buscar sin acentos tiene que encontrar lo que los tiene.
     *
     * Nadie los escribe en un buscador, y la mitad de los barrios de la Ciudad los
     * tienen: "Atiko" no encontraba a "Átiko - Agronomia", ni "Nunez" a los de Núñez.
     */
    @Test
    void buscarSinAcentosEncuentraLoQueLosTiene() {
        guardar("Átiko - Agronomía", "Palermo", false);

        assertThat(nombres("atiko", (List<String>) null)).containsExactly("Átiko - Agronomía");
        assertThat(nombres("Atiko", (List<String>) null)).containsExactly("Átiko - Agronomía");
        assertThat(nombres("agronomia", (List<String>) null)).containsExactly("Átiko - Agronomía");
    }

    /** Y al revés: escribirlos con acento también tiene que encontrar. */
    @Test
    void buscarConAcentosTambienEncuentra() {
        guardar("Átiko - Agronomía", "Palermo", false);

        assertThat(nombres("Átiko", (List<String>) null)).containsExactly("Átiko - Agronomía");
    }

    /** Cambiarle el nombre a un local tiene que dejarlo encontrable por el nuevo. */
    @Test
    void alCambiarleElNombreSeLoEncuentraPorElNuevo() {
        BurgerJoint local = repository.save(BurgerJoint.builder()
            .name("Menganito").address("Una dirección").area("Palermo").fastFood(false)
            .build());

        local.setName("Ñandú Burger");
        repository.saveAndFlush(local);

        assertThat(nombres("nandu", (List<String>) null)).containsExactly("Ñandú Burger");
    }

    /** Los barrios que se ofrecen en el selector son los que tienen algún local. */
    @Test
    void elSelectorOfreceLosBarriosQueTienenLocales() {
        assertThat(service.barrios()).containsExactly("Boedo", "Palermo");
    }

    // ---- varios barrios a la vez ----

    /** Dos barrios traen los de los dos, que es lo que quiere quien marca dos. */
    @Test
    void dosBarriosTraenLosDeLosDos() {
        assertThat(nombres(null, List.of("Boedo", "Palermo")))
            .containsExactlyInAnyOrder("The Burger Company", "Voraz", "Burger de Boedo");
    }

    /**
     * Se leen como "o", no como "y".
     *
     * Un local está en un solo barrio, así que pedir los que estén en dos a la vez no
     * devolvería nunca nada. Es el error que haría que la pantalla saliera vacía con dos
     * barrios marcados.
     */
    @Test
    void variosBarriosSeLeenComoO() {
        assertThat(nombres(null, List.of("Boedo", "Palermo")))
            .containsExactlyInAnyOrder("The Burger Company", "Voraz", "Burger de Boedo");
    }

    /**
     * Y el total de la paginación acompaña, o la lista queda con huecos.
     *
     * Se compara contra el de un solo barrio: lo que hay que ver es que sumar un barrio
     * suma sus locales al total, no solo que el número sea alguno.
     */
    @Test
    void elTotalCreceAlSumarUnBarrio() {
        long soloBoedo = service.search(null, List.of("Boedo"), OrdenDeLocales.RELEVANTES, null,
            PageRequest.of(0, 20)).getTotalElements();
        long boedoYPalermo = service.search(null, List.of("Boedo", "Palermo"), OrdenDeLocales.RELEVANTES, null,
            PageRequest.of(0, 20)).getTotalElements();

        assertThat(soloBoedo).isEqualTo(2);
        assertThat(boedoYPalermo).isEqualTo(3);
    }

    /** Los filtros se siguen cruzando: barrios y nombre a la vez. */
    @Test
    void losBarriosSeCruzanConElNombre() {
        assertThat(nombres("burger", List.of("Boedo", "Palermo")))
            .containsExactlyInAnyOrder("The Burger Company", "Burger de Boedo");
    }

    /** Un barrio que no existe no trae nada, y no rompe. */
    @Test
    void unBarrioQueNoExisteNoTraeNada() {
        assertThat(nombres(null, List.of("Marte"))).isEmpty();
    }

    /**
     * Una lista con un barrio vacío muestra todo, no nada.
     *
     * Es lo que llega de un "?area=" suelto en la dirección. Si pasara al filtro, la
     * pantalla saldría vacía sin que se vea por qué.
     */
    @Test
    void unBarrioVacioMuestraTodo() {
        assertThat(nombres(null, List.of(""))).hasSize(3);
    }
}
