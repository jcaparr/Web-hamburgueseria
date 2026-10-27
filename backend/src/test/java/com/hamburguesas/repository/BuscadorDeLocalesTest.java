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
 * El buscador de la pantalla de reseña contra H2 (#335): cada palabra tiene que estar en
 * el nombre o en el barrio, sin acentos ni mayúsculas, y entran las cadenas.
 *
 * Explorar no cambia: busca la frase entera solo contra el nombre y no muestra cadenas.
 * Los tests de {@code explorar...} lo fijan.
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

    /** Lo que hace la pantalla de reseña. */
    private List<String> paraResenia(String q) {
        return repository.buscar(q, null, OrdenDeLocales.RELEVANTES, null, true, PageRequest.of(0, 20))
            .map(BurgerJoint::getName).getContent();
    }

    /** Lo que hace Explorar, igual que antes de #335. */
    private List<String> explorar(String q) {
        return repository.buscar(q, null, OrdenDeLocales.RELEVANTES, null, PageRequest.of(0, 20))
            .map(BurgerJoint::getName).getContent();
    }

    @Test
    void cadaPalabraPuedeEstarEnElNombreOEnElBarrio() {
        local("a", "Átiko", "Núñez", false);
        local("b", "Átiko", "Palermo", false);

        // Una palabra en el nombre y otra en el barrio, sin acentos ni mayúsculas.
        assertThat(repository.buscar("ATIKO nunez", null, OrdenDeLocales.RELEVANTES, null, true, PageRequest.of(0, 20))
            .map(BurgerJoint::getArea).getContent()).containsExactly("Núñez");
        // El orden de las palabras no importa.
        assertThat(paraResenia("palermo atiko")).containsExactly("Átiko");
        // Solo el barrio también encuentra.
        assertThat(paraResenia("nunez")).containsExactly("Átiko");
    }

    @Test
    void todasLasPalabrasTienenQueAparecer() {
        local("c", "La Birra Bar", "Belgrano", false);

        assertThat(paraResenia("birra belgrano")).hasSize(1);
        assertThat(paraResenia("birra palermo")).isEmpty();
    }

    @Test
    void paraReseniarEntranLasCadenas() {
        local("d", "Mostaza", "Belgrano", true);

        assertThat(paraResenia("mostaza belgrano")).containsExactly("Mostaza");
    }

    @Test
    void explorarNoCambia() {
        local("x", "Átiko", "Núñez", false);
        local("y", "Mostaza", "Belgrano", true);

        // El barrio no se busca: es la frase entera contra el nombre.
        assertThat(explorar("nunez")).isEmpty();
        assertThat(explorar("atiko nunez")).isEmpty();
        assertThat(explorar("belgrano")).isEmpty();
        // El nombre sí, sin acentos, y con la frase entera.
        assertThat(explorar("ATIKO")).containsExactly("Átiko");
        // Las cadenas siguen sin salir (#206).
        assertThat(explorar("mostaza")).isEmpty();
    }

    @Test
    void losEspaciosDeMasYLosComodinesNoRompen() {
        local("e", "Burger 100%", "Caballito", false);

        assertThat(paraResenia("  burger   caballito ")).hasSize(1);
        assertThat(paraResenia("%")).hasSize(1);
        assertThat(paraResenia("_urger")).isEmpty();
        assertThat(explorar("%")).hasSize(1);
        assertThat(explorar("_urger")).isEmpty();
    }

    @Test
    void elBarrioSeActualizaAlGuardar() {
        BurgerJoint l = local("f", "Cambia", "Flores", false);
        l.setArea("Almagro");
        repository.save(l);

        assertThat(paraResenia("cambia flores")).isEmpty();
        assertThat(paraResenia("cambia almagro")).hasSize(1);
    }

    @Test
    void unBarrioNuloOConEnieYSoloEspaciosNoRompen() {
        local("g", "Sin Barrio", null, false);
        local("h", "Con Eñe", "Cañuelas", false);

        assertThat(paraResenia("sin barrio")).containsExactly("Sin Barrio");
        assertThat(paraResenia("con canuelas")).containsExactly("Con Eñe");
        // Solo espacios es no buscar nada: devuelve todo.
        assertThat(paraResenia("   ")).contains("Sin Barrio", "Con Eñe");
    }
}
