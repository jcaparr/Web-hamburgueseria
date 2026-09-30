package com.hamburguesas.places;

import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.repository.BurgerJointRepository;
import com.hamburguesas.repository.RatingRepository;
import com.hamburguesas.repository.SavedTourRepository;
import com.hamburguesas.repository.WishlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cubre la revisión de fotos suelta, sin la búsqueda de locales.
 *
 * Existe porque cambiar la regla de elección no cambia ninguna foto por sí solo: lo
 * guardado se queda como está hasta que alguien vuelva a mirarlo. Y hacerlo con la
 * sincronización completa gasta además hasta mil búsquedas recorriendo los 48 barrios,
 * que es la parte cara y la que acá no hace falta.
 *
 * No sale a internet: el cliente de Google está simulado.
 */
class RevisionDeFotosTest {

    private PlacesClient placesClient;
    private BurgerJointRepository repository;
    private PhotoStorage photoStorage;
    private PlacesSyncService service;

    @BeforeEach
    void setUp() {
        PlacesProperties properties = new PlacesProperties();
        properties.setApiKey("clave-de-prueba");
        properties.getSync().setDelayBetweenCallsMs(0);

        placesClient = mock(PlacesClient.class);
        repository = mock(BurgerJointRepository.class);
        photoStorage = mock(PhotoStorage.class);
        PlacesQuotaGuard quotaGuard = mock(PlacesQuotaGuard.class);

        when(quotaGuard.canCall(any())).thenReturn(true);
        when(repository.findByPhotoUrlIsNull()).thenReturn(List.of());
        when(repository.findByPhotoUrlIsNotNull()).thenReturn(List.of());
        when(repository.conFotoElegidaConUnaReglaVieja(anyInt())).thenReturn(List.of());
        when(repository.findByPlaceId(anyString())).thenReturn(Optional.empty());
        when(repository.nombresDeRubro(anyString())).thenReturn(List.of());

        service = new PlacesSyncService(
            properties, placesClient, quotaGuard, photoStorage, repository, new Barrios(),
            mock(RatingRepository.class), mock(WishlistRepository.class),
            mock(SavedTourRepository.class), new FastFoodMarker(repository, properties));
    }

    private BurgerJoint local(String nombre, String photoUrl, Integer regla) {
        return BurgerJoint.builder()
            .id(1L).placeId("ChIJ" + nombre).name(nombre)
            .address("Una dirección").area("Palermo")
            .photoUrl(photoUrl).photoName(photoUrl == null ? null : "places/x/photos/vieja")
            .photoRule(regla)
            .build();
    }

    private BurgerJoint guardado() {
        ArgumentCaptor<BurgerJoint> capturado = ArgumentCaptor.forClass(BurgerJoint.class);
        verify(repository).save(capturado.capture());
        return capturado.getValue();
    }

    /** Lo que separa esto de la sincronización completa: no gasta ni una búsqueda. */
    @Test
    void noSaleABuscarLocalesNuevos() {
        service.revisarFotos();

        verify(placesClient, never()).searchText(anyString(), any());
    }

    /**
     * Un local del que Google no tiene ni una foto queda anotado como tal.
     *
     * Es lo único que distingue "no hay nada" de "no fuimos a buscarlo": de los 102 que
     * no tienen foto guardada, la mitad sí las tiene en Google. Confundirlos escondería
     * medio centenar de hamburgueserías reales por un trabajo nuestro a medias.
     */
    @Test
    void anotaAlLocalDelQueGoogleNoTieneNingunaFoto() {
        when(repository.findByPhotoUrlIsNull()).thenReturn(List.of(local("Sin nada", null, null)));
        when(placesClient.photoNameFor(anyString())).thenReturn(null);

        service.revisarFotos();

        assertThat(guardado().isSinFotosEnGoogle()).isTrue();
    }

    /** Y si aparece una foto, se le borra la anotación: un local nuevo hoy no tiene y mañana sí. */
    @Test
    void siAparecenFotosSeLeBorraLaAnotacion() {
        BurgerJoint local = local("Ya tiene", null, null);
        local.setSinFotosEnGoogle(true);
        when(repository.findByPhotoUrlIsNull()).thenReturn(List.of(local));
        when(placesClient.photoNameFor(anyString())).thenReturn("places/x/photos/nueva");
        when(placesClient.downloadPhoto(anyString())).thenReturn(new byte[] {1, 2, 3});
        when(photoStorage.save(anyString(), any())).thenReturn("/api/place-photos/x.jpg");

        service.revisarFotos();

        assertThat(guardado().isSinFotosEnGoogle()).isFalse();
    }

    /**
     * La foto que la regla nueva elige igual que la vieja no se vuelve a bajar.
     *
     * Sería pagarle a Google por el mismo archivo. Alcanza con anotar que ya se revisó,
     * así el trabajo se hace una vez y no en cada corrida.
     */
    @Test
    void siLaReglaNuevaEligeLaMismaFotoNoLaVuelveABajar() {
        BurgerJoint local = local("Igual", "/api/place-photos/vieja.jpg", 2);
        when(repository.conFotoElegidaConUnaReglaVieja(anyInt())).thenReturn(List.of(local));
        when(placesClient.photoNameFor(anyString())).thenReturn("places/x/photos/vieja");

        service.revisarFotos();

        verify(placesClient, never()).downloadPhoto(anyString());
        assertThat(guardado().getPhotoRule()).isEqualTo(PlacesClient.REGLA_DE_FOTO);
    }

    /** Y si elige otra, esa se baja y reemplaza a la anterior. */
    @Test
    void siLaReglaNuevaEligeOtraFotoLaReemplaza() {
        BurgerJoint local = local("Cambia", "/api/place-photos/vieja.jpg", 2);
        when(repository.conFotoElegidaConUnaReglaVieja(anyInt())).thenReturn(List.of(local));
        when(placesClient.photoNameFor(anyString())).thenReturn("places/x/photos/mejor");
        when(placesClient.downloadPhoto(anyString())).thenReturn(new byte[] {1, 2, 3});
        when(photoStorage.save(anyString(), any())).thenReturn("/api/place-photos/mejor.jpg");

        service.revisarFotos();

        BurgerJoint despues = guardado();
        assertThat(despues.getPhotoUrl()).isEqualTo("/api/place-photos/mejor.jpg");
        assertThat(despues.getPhotoName()).isEqualTo("places/x/photos/mejor");
        assertThat(despues.getPhotoRule()).isEqualTo(PlacesClient.REGLA_DE_FOTO);
    }
}
