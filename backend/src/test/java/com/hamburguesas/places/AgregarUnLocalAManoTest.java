package com.hamburguesas.places;

import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.model.PlacesCallType;
import com.hamburguesas.repository.BurgerJointRepository;
import com.hamburguesas.repository.RatingRepository;
import com.hamburguesas.repository.SavedTourRepository;
import com.hamburguesas.repository.WishlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Agregar una hamburguesería que el barrido no encuentra.
 *
 * "Austin's Diner & Grill" es el caso de manual: Google no le pone el rubro de
 * hamburguesería, así que la búsqueda estricta por barrio no lo devuelve por más veces
 * que se corra. Sin esto, la única forma de meterlo era a mano en la base.
 */
class AgregarUnLocalAManoTest {

    private PlacesClient placesClient;
    private BurgerJointRepository repository;
    private PlacesQuotaGuard quotaGuard;
    private PlacesSyncService service;

    @BeforeEach
    void setUp() {
        PlacesProperties properties = new PlacesProperties();
        properties.setApiKey("clave-de-prueba");
        properties.getSync().setDelayBetweenCallsMs(0);

        placesClient = mock(PlacesClient.class);
        repository = mock(BurgerJointRepository.class);
        quotaGuard = mock(PlacesQuotaGuard.class);

        when(quotaGuard.canCall(any())).thenReturn(true);
        when(repository.findByPlaceId(anyString())).thenReturn(Optional.empty());
        when(repository.findByFastFoodFalse()).thenReturn(List.of());
        when(repository.nombresDeRubro(anyString())).thenReturn(List.of());
        when(repository.conFotoElegidaConUnaReglaVieja(anyInt())).thenReturn(List.of());

        service = new PlacesSyncService(
            properties, placesClient, quotaGuard, mock(PhotoStorage.class), repository,
            new Barrios(), new Zonas(new Barrios(), properties), mock(RatingRepository.class),
            mock(WishlistRepository.class), mock(SavedTourRepository.class),
            new FastFoodMarker(repository, properties));
    }

    /** Un local de Palermo con un rubro que el clasificador rechazaría. */
    private void googleDevuelve(String nombre, double lat, double lon, String rubro) {
        when(placesClient.searchText(anyString(), any())).thenReturn(new PlacesSearchResult(
            List.of(new PlacesSearchResult.Place(
                "ChIJ-austin", nombre, "Costa Rica 5827, CABA", lat, lon,
                "places/x/photos/una", "1200x900|Alguien", rubro, Set.of(rubro))),
            null));
    }

    private BurgerJoint guardado() {
        ArgumentCaptor<BurgerJoint> capturado = ArgumentCaptor.forClass(BurgerJoint.class);
        verify(repository).save(capturado.capture());
        return capturado.getValue();
    }

    /**
     * Entra aunque el clasificador diría que no.
     *
     * Es el punto del endpoint: lo agrega alguien que ya sabe que el local existe y
     * vende hamburguesas, y esa decisión vale más que cualquier regla nuestra.
     */
    @Test
    void entraAunqueGoogleNoLoLlameHamburgueseria() {
        googleDevuelve("Austin's Diner & Grill", -34.5900, -58.4270, "restaurant");

        PlacesSyncService.LocalAgregado resultado = service.agregar("Austin's Diner & Grill");

        assertThat(resultado.resultado()).isEqualTo("Agregado");
        assertThat(guardado().getName()).isEqualTo("Austin's Diner & Grill");
        assertThat(guardado().getArea()).isEqualTo("Palermo");
    }

    /**
     * Devuelve el identificador, que es lo que hay que anotar después.
     *
     * Un local agregado a mano suele ser uno que ninguna regla reconoce, así que la
     * próxima limpieza lo borraría si no queda anotado en included-place-ids.
     */
    @Test
    void devuelveElIdentificadorParaPoderAnotarlo() {
        googleDevuelve("Austin's Diner & Grill", -34.5900, -58.4270, "restaurant");

        assertThat(service.agregar("Austin's").placeId()).isEqualTo("ChIJ-austin");
    }

    /** No baja la foto: la cuota de fotos es el tramo más chico y esto se usa de a uno. */
    @Test
    void noBajaLaFoto() {
        googleDevuelve("Austin's Diner & Grill", -34.5900, -58.4270, "restaurant");

        service.agregar("Austin's");

        verify(placesClient, never()).downloadPhoto(anyString());
        verify(quotaGuard, never()).record(PlacesCallType.PHOTO);
    }

    /** El radio sí se respeta: fuera de los 75 km la app no lo podría ubicar. */
    @Test
    void noEntraSiEstaFueraDelRadio() {
        googleDevuelve("Burger de Mar del Plata", -37.9619, -57.5602, "hamburger_restaurant");

        PlacesSyncService.LocalAgregado resultado = service.agregar("Burger de Mar del Plata");

        assertThat(resultado.resultado()).contains("fuera del radio");
        verify(repository, never()).save(any(BurgerJoint.class));
    }

    /** Pedirlo dos veces no lo duplica. */
    @Test
    void siYaEstaNoLoDuplica() {
        googleDevuelve("Austin's Diner & Grill", -34.5900, -58.4270, "restaurant");
        when(repository.findByPlaceId("ChIJ-austin")).thenReturn(Optional.of(
            BurgerJoint.builder().id(1L).placeId("ChIJ-austin").name("Austin's Diner & Grill")
                .address("Costa Rica 5827").area("Palermo").build()));

        PlacesSyncService.LocalAgregado resultado = service.agregar("Austin's");

        assertThat(resultado.resultado()).isEqualTo("Ya estaba");
        verify(repository, never()).save(any(BurgerJoint.class));
    }

    /** Si Google no encuentra nada, lo dice en vez de fallar. */
    @Test
    void siGoogleNoEncuentraNadaLoDice() {
        when(placesClient.searchText(anyString(), any()))
            .thenReturn(new PlacesSearchResult(List.of(), null));

        assertThat(service.agregar("algo que no existe").resultado())
            .isEqualTo("Google no encontró nada");
    }

    /** Y sin cuota de búsquedas tampoco sale a buscar. */
    @Test
    void sinCuotaDeBusquedasNoSale() {
        when(quotaGuard.canCall(PlacesCallType.SEARCH)).thenReturn(false);

        assertThat(service.agregar("Austin's").resultado()).contains("cuota");
        verify(placesClient, never()).searchText(anyString(), any());
    }
}
