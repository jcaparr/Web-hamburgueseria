package com.hamburguesas.service;

import com.hamburguesas.dto.BurgerJointDto;
import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.repository.BurgerJointRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cubre los filtros de Explorar: el nombre, el barrio y las cadenas.
 *
 * De los 442 locales, 109 son sucursales de cadenas, y entre McDonald's y Burger King
 * ocupan tres páginas enteras de la lista. El barrio es la otra forma de achicarla: la
 * Ciudad tiene 48 y uno come en el suyo.
 *
 * Lo que más importa acá es que los tres se combinen. Eran consultas separadas, una por
 * combinación, y sumar un filtro más multiplicaba la cuenta.
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

    private List<String> nombres(String query, String barrio, boolean conCadenas) {
        return service.search(query, barrio, conCadenas, null, PageRequest.of(0, 20))
            .map(BurgerJointDto::name)
            .toList();
    }

    // ---- las cadenas ----

    @Test
    void conLasCadenasPrendidasEstanTodos() {
        assertThat(nombres(null, null, true)).containsExactlyInAnyOrder(
            "McDonald's", "Burger King", "The Burger Company", "Voraz", "Burger de Boedo");
    }

    @Test
    void apagadasQuedanSoloLasQueNoSonCadena() {
        assertThat(nombres(null, null, false))
            .containsExactlyInAnyOrder("The Burger Company", "Voraz", "Burger de Boedo");
    }

    /** El total de la paginación también tiene que bajar, o la lista queda con huecos. */
    @Test
    void elTotalReflejaElFiltro() {
        assertThat(service.search(null, null, true, null, PageRequest.of(0, 20)).getTotalElements())
            .isEqualTo(5);
        assertThat(service.search(null, null, false, null, PageRequest.of(0, 20)).getTotalElements())
            .isEqualTo(3);
        assertThat(service.search(null, "Boedo", true, null, PageRequest.of(0, 20)).getTotalElements())
            .isEqualTo(2);
    }

    // ---- el barrio ----

    @Test
    void elBarrioDejaSoloLosDeEseBarrio() {
        assertThat(nombres(null, "Boedo", true))
            .containsExactlyInAnyOrder("Voraz", "Burger de Boedo");
    }

    @Test
    void sinBarrioSeVeLaCiudadEntera() {
        assertThat(nombres(null, null, true)).hasSize(5);
        assertThat(nombres(null, "", true)).hasSize(5);
    }

    @Test
    void unBarrioSinLocalesNoDevuelveNada() {
        assertThat(nombres(null, "Mataderos", true)).isEmpty();
    }

    // ---- los tres juntos ----

    /**
     * Los tres filtros se aplican a la vez, que es lo que hace un listado usable: "una
     * hamburguesería que se llame burger, en Boedo, que no sea una cadena".
     */
    @Test
    void losTresFiltrosSeCombinan() {
        assertThat(nombres("burger", null, true))
            .containsExactlyInAnyOrder("Burger King", "The Burger Company", "Burger de Boedo");
        assertThat(nombres("burger", "Boedo", true))
            .containsExactly("Burger de Boedo");
        assertThat(nombres("burger", "Palermo", false))
            .containsExactly("The Burger Company");
    }

    /**
     * El % y el _ son comodines del LIKE, y el buscador los deja escribir.
     *
     * Sin escaparlos, buscar "%" devolvía el listado entero, que es exactamente lo
     * contrario de lo que esperaría alguien que escribió un carácter.
     */
    @Test
    void losComodinesDelBuscadorSonTextoYNoComodines() {
        assertThat(nombres("%", null, true)).isEmpty();
        assertThat(nombres("_", null, true)).isEmpty();
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

        assertThat(nombres("atiko", null, true)).containsExactly("Átiko - Agronomía");
        assertThat(nombres("Atiko", null, true)).containsExactly("Átiko - Agronomía");
        assertThat(nombres("agronomia", null, true)).containsExactly("Átiko - Agronomía");
    }

    /** Y al revés: escribirlos con acento también tiene que encontrar. */
    @Test
    void buscarConAcentosTambienEncuentra() {
        guardar("Átiko - Agronomía", "Palermo", false);

        assertThat(nombres("Átiko", null, true)).containsExactly("Átiko - Agronomía");
    }

    /** Cambiarle el nombre a un local tiene que dejarlo encontrable por el nuevo. */
    @Test
    void alCambiarleElNombreSeLoEncuentraPorElNuevo() {
        BurgerJoint local = repository.save(BurgerJoint.builder()
            .name("Menganito").address("Una dirección").area("Palermo").fastFood(false)
            .build());

        local.setName("Ñandú Burger");
        repository.saveAndFlush(local);

        assertThat(nombres("nandu", null, true)).containsExactly("Ñandú Burger");
    }

    /** Los barrios que se ofrecen en el selector son los que tienen algún local. */
    @Test
    void elSelectorOfreceLosBarriosQueTienenLocales() {
        assertThat(service.barrios()).containsExactly("Boedo", "Palermo");
    }
}
