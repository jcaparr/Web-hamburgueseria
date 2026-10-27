package com.hamburguesas.repository;

import com.hamburguesas.model.BurgerJoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El buscador de locales contra H2 (#335): cada palabra tiene que estar en el nombre o
 * en el barrio, sin acentos ni mayúsculas, y las cadenas solo salen si se piden.
 */
@SpringBootTest
@ActiveProfiles("test")
class BuscadorDeLocalesTest {

    @Autowired private BurgerJointRepository repository;

    @BeforeEach
    @AfterEach
    void limpiar() {
        repository.findAll().stream().filter(l -> l.getPlaceId() != null && l.getPlaceId().startsWith("bus-")).forEach(repository::delete);
    }

    private BurgerJoint local(String id, String nombre, String barrio, boolean cadena) {
        return repository.save(BurgerJoint.builder().name(nombre).address("Calle 1").area(barrio)
            .fastFood(cadena).slug("bus-" + id).placeId("bus-" + id).build());
    }

    private List<String> buscar(String q, boolean conCadenas) {
        return repository.buscar(q, null, OrdenDeLocales.RELEVANTES, null, conCadenas, PageRequest.of(0, 20))
            .map(BurgerJoint::getName).getContent();
    }

    @Test
    void cadaPalabraPuedeEstarEnElNombreOEnElBarrio() {
        local("a", "Átiko", "Núñez", false);
        local("b", "Átiko", "Palermo", false);

        // Una palabra en el nombre y otra en el barrio, sin acentos ni mayúsculas.
        assertThat(repository.buscar("ATIKO nunez", null, OrdenDeLocales.RELEVANTES, null, PageRequest.of(0, 20))
            .map(BurgerJoint::getArea).getContent()).containsExactly("Núñez");
        // El orden de las palabras no importa.
        assertThat(buscar("palermo atiko", false)).containsExactly("Átiko");
        // Solo el barrio también encuentra.
        assertThat(buscar("nunez", false)).containsExactly("Átiko");
    }

    @Test
    void todasLasPalabrasTienenQueAparecer() {
        local("c", "La Birra Bar", "Belgrano", false);

        assertThat(buscar("birra belgrano", false)).hasSize(1);
        assertThat(buscar("birra palermo", false)).isEmpty();
    }

    @Test
    void lasCadenasSoloSalenSiSePiden() {
        local("d", "Mostaza", "Belgrano", true);

        assertThat(buscar("mostaza belgrano", false)).isEmpty();
        assertThat(buscar("mostaza belgrano", true)).containsExactly("Mostaza");
    }

    @Test
    void losEspaciosDeMasYLosComodinesNoRompen() {
        local("e", "Burger 100%", "Caballito", false);

        assertThat(buscar("  burger   caballito ", false)).hasSize(1);
        assertThat(buscar("%", false)).hasSize(1);
        assertThat(buscar("_urger", false)).isEmpty();
    }

    @Test
    void elBarrioSeActualizaAlGuardar() {
        BurgerJoint l = local("f", "Cambia", "Flores", false);
        l.setArea("Almagro");
        repository.save(l);

        assertThat(buscar("cambia flores", false)).isEmpty();
        assertThat(buscar("cambia almagro", false)).hasSize(1);
    }

    @Test
    void unBarrioNuloOConEnieYSoloEspaciosNoRompen() {
        local("g", "Sin Barrio", null, false);
        local("h", "Con Eñe", "Cañuelas", false);

        assertThat(buscar("sin barrio", false)).containsExactly("Sin Barrio");
        assertThat(buscar("con canuelas", false)).containsExactly("Con Eñe");
        // Solo espacios es no buscar nada: devuelve todo lo que no es cadena.
        assertThat(buscar("   ", false)).contains("Sin Barrio", "Con Eñe");
    }
}
