package com.hamburguesas.service;

import com.hamburguesas.dto.CadenaDto;
import com.hamburguesas.exception.ResourceNotFoundException;
import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.places.PlacesProperties;
import com.hamburguesas.repository.BurgerJointRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Las cadenas se ven buscándolas por nombre, una tarjeta por cadena (#206).
 *
 * Con el repositorio simulado: lo que se prueba es cómo se agrupan y se cuentan, y otras
 * clases de test dejan sucursales de cadenas en la base compartida.
 */
class CadenasServiceTest {

    private BurgerJointRepository repository;
    private FichaDeLocal fichaDeLocal;
    private CadenasService service;
    private final List<BurgerJoint> sucursales = new ArrayList<>();
    private long proximoId = 1;

    @BeforeEach
    void setUp() {
        PlacesProperties properties = new PlacesProperties();
        properties.setFastFoodBrands(List.of("mcdonalds", "burgerking", "mostaza", "mrtasty"));
        properties.setNombresDeCadenas(Map.of(
            "mcdonalds", "McDonald's", "burgerking", "Burger King", "mostaza", "Mostaza"));

        repository = mock(BurgerJointRepository.class);
        fichaDeLocal = mock(FichaDeLocal.class);
        when(repository.findByFastFoodTrue()).thenReturn(sucursales);
        when(fichaDeLocal.para(anyList(), any())).thenReturn(List.of());

        sucursal("McDonald's", "Palermo", "/api/place-photos/mc.jpg");
        sucursal("McDonald's Abasto Patio de Comidas", "Almagro", "/api/place-photos/mc.jpg");
        sucursal("MC DONALDS", "Belgrano", "/api/place-photos/otra.jpg");
        sucursal("Burger King - Sucursal P.Italia", "Palermo", null);
        sucursal("Mostaza", "Once", "/api/place-photos/mostaza.jpg");
        sucursal("Mr. Tasty", "Flores", null);
        sucursal("Mr Tasty Caballito", "Caballito", null);

        service = new CadenasService(repository, properties, fichaDeLocal);
    }

    private void sucursal(String nombre, String barrio, String foto) {
        sucursales.add(BurgerJoint.builder()
            .id(proximoId++).name(nombre).area(barrio).address("Una calle").photoUrl(foto)
            .fastFood(true).build());
    }

    @Test
    void buscarMcDevuelveUnaSolaTarjetaConTodasLasSucursales() {
        assertThat(service.buscar("mc", null)).singleElement().satisfies(cadena -> {
            assertThat(cadena.marca()).isEqualTo("mcdonalds");
            assertThat(cadena.nombre()).isEqualTo("McDonald's");
            assertThat(cadena.sucursales()).isEqualTo(3);
        });
    }

    /** Sin espacios, signos ni acentos: como sea que la escriba quien busca. */
    @Test
    void laEncuentraEscritaDeCualquierForma() {
        assertThat(service.buscar("Mc Donald's", null)).extracting(CadenaDto::marca).containsExactly("mcdonalds");
        assertThat(service.buscar("king", null)).extracting(CadenaDto::marca).containsExactly("burgerking");
        assertThat(service.buscar("mostaza palermo", null)).extracting(CadenaDto::marca).containsExactly("mostaza");
    }

    @Test
    void conMenosDeDosLetrasNoBusca() {
        assertThat(service.buscar("m", null)).isEmpty();
        assertThat(service.buscar("  ", null)).isEmpty();
        assertThat(service.buscar(null, null)).isEmpty();
    }

    @Test
    void loQueNoEsUnaCadenaNoTraeNinguna() {
        assertThat(service.buscar("voraz", null)).isEmpty();
    }

    /** Con barrios elegidos, cuenta las de ahí; y si no hay ninguna, la cadena no sale. */
    @Test
    void conBarriosCuentaSoloLasDeEsosBarrios() {
        assertThat(service.buscar("mc", List.of("Palermo", "Almagro")))
            .singleElement().extracting(CadenaDto::sucursales).isEqualTo(2L);
        assertThat(service.buscar("mc", List.of("Marte"))).isEmpty();
        assertThat(service.buscar("mc", List.of(""))).singleElement()
            .extracting(CadenaDto::sucursales).isEqualTo(3L);
    }

    /** Varias a la vez, primero la que más sucursales tiene. */
    @Test
    void variasCadenasVanDeLaMasGrandeALaMasChica() {
        assertThat(service.buscar("ta", null)).extracting(CadenaDto::marca)
            .containsExactly("mrtasty", "mostaza");
    }

    /** La portada es la que más sucursales comparten. */
    @Test
    void laPortadaEsLaMasRepetida() {
        assertThat(service.buscar("mc", null)).singleElement()
            .extracting(CadenaDto::fotoUrl).isEqualTo("/api/place-photos/mc.jpg");
        assertThat(service.buscar("king", null)).singleElement()
            .extracting(CadenaDto::fotoUrl).isNull();
    }

    /** Sin nombre anotado sale la clave: se lee peor, pero la tarjeta no queda vacía. */
    @Test
    void sinNombreAnotadoUsaLaClave() {
        assertThat(service.buscar("tasty", null)).singleElement()
            .extracting(CadenaDto::nombre).isEqualTo("mrtasty");
    }

    @SuppressWarnings("unchecked")
    @Test
    void laPaginaDeLaCadenaTraeSusSucursalesPorBarrio() {
        var pagina = service.sucursales("mcdonalds", 7L);

        ArgumentCaptor<List<BurgerJoint>> pasadas = ArgumentCaptor.forClass(List.class);
        verify(fichaDeLocal).para(pasadas.capture(), any());
        assertThat(pasadas.getValue()).extracting(BurgerJoint::getArea)
            .containsExactly("Almagro", "Belgrano", "Palermo");
        assertThat(pagina.nombre()).isEqualTo("McDonald's");
    }

    @Test
    void unaCadenaQueNoExisteEs404() {
        assertThatThrownBy(() -> service.sucursales("voraz", null))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
