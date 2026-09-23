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
 * Cubre el filtro de cadenas de comida rápida en Explorar.
 *
 * De los 417 locales, 66 son sucursales de McDonald's, Burger King, Mostaza y Wendy's,
 * y entre las dos primeras ocupan tres páginas enteras de la lista.
 *
 * Contra una base de verdad, porque lo que se prueba son las consultas.
 */
@SpringBootTest
@ActiveProfiles("test")
class FiltroDeCadenasTest {

    @Autowired private BurgerJointService service;
    @Autowired private BurgerJointRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        guardar("McDonald's", true);
        guardar("Burger King", true);
        guardar("The Burger Company", false);
        guardar("Voraz", false);
    }

    private void guardar(String nombre, boolean cadena) {
        repository.save(BurgerJoint.builder()
            .name(nombre).address("Una dirección").area("Palermo")
            .fastFood(cadena)
            .build());
    }

    private List<String> nombres(String query, boolean conCadenas) {
        return service.search(query, conCadenas, null, PageRequest.of(0, 20))
            .map(BurgerJointDto::name)
            .toList();
    }

    @Test
    void conLasCadenasPrendidasEstanTodos() {
        assertThat(nombres(null, true))
            .containsExactlyInAnyOrder("McDonald's", "Burger King", "The Burger Company", "Voraz");
    }

    @Test
    void apagadasQuedanSoloLasQueNoSonCadena() {
        assertThat(nombres(null, false))
            .containsExactlyInAnyOrder("The Burger Company", "Voraz");
    }

    /** El total de la paginación también tiene que bajar, o la lista queda con huecos. */
    @Test
    void elTotalReflejaElFiltro() {
        assertThat(service.search(null, true, null, PageRequest.of(0, 20)).getTotalElements()).isEqualTo(4);
        assertThat(service.search(null, false, null, PageRequest.of(0, 20)).getTotalElements()).isEqualTo(2);
    }

    /** El filtro y la búsqueda por nombre se aplican juntos, no uno o el otro. */
    @Test
    void elFiltroConviveConLaBusquedaPorNombre() {
        assertThat(nombres("burger", true))
            .containsExactlyInAnyOrder("Burger King", "The Burger Company");
        assertThat(nombres("burger", false))
            .containsExactly("The Burger Company");
    }
}
