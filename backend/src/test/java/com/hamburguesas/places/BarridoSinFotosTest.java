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
 * El barrido que trae locales sin bajar ni una foto.
 *
 * Las dos cuotas son muy desparejas: recorrer las 89 zonas sale unas mil doscientas
 * búsquedas de las cuatro mil del mes, y ponerle portada a todo lo que entra no alcanza
 * ni de cerca con las mil fotos. Separarlas deja traer los locales ahora y decidir
 * después a quién se le gasta una foto, con los que Google no tiene fotografiados ya
 * borrados.
 *
 * Lo que se prueba acá es que apagado no baje nada por ninguno de los tres caminos
 * —crear, completar y recambiar— y que prendido siga bajando como siempre.
 */
class BarridoSinFotosTest {

    private PlacesProperties properties;
    private PlacesClient placesClient;
    private BurgerJointRepository repository;
    private PhotoStorage photoStorage;
    private PlacesQuotaGuard quotaGuard;
    private PlacesSyncService service;

    @BeforeEach
    void setUp() {
        properties = new PlacesProperties();
        properties.setApiKey("clave-de-prueba");
        properties.getSync().setAreas(List.of("Villa Real"));
        properties.getSync().setMaxPagesPerArea(1);
        properties.getSync().setQueryTemplates(List.of("hamburguesería en {barrio}"));
        properties.getSync().setDelayBetweenCallsMs(0);

        placesClient = mock(PlacesClient.class);
        quotaGuard = mock(PlacesQuotaGuard.class);
        photoStorage = mock(PhotoStorage.class);
        repository = mock(BurgerJointRepository.class);

        when(quotaGuard.canCall(any())).thenReturn(true);
        when(repository.findByPhotoUrlIsNull()).thenReturn(List.of());
        when(repository.findByPhotoUrlIsNotNull()).thenReturn(List.of());
        when(repository.conFotoElegidaConUnaReglaVieja(anyInt())).thenReturn(List.of());
        when(repository.findByPlaceId(anyString())).thenReturn(Optional.empty());
        when(repository.nombresDeRubro(anyString())).thenReturn(List.of());
        when(photoStorage.save(anyString(), any())).thenReturn("/api/place-photos/x.jpg");

        service = new PlacesSyncService(
            properties, placesClient, quotaGuard, photoStorage, repository, new Barrios(),
            new Zonas(new Barrios(), properties), mock(RatingRepository.class),
            mock(WishlistRepository.class), mock(SavedTourRepository.class),
            new FastFoodMarker(repository, properties));
    }

    /** Un local de Palermo, con foto disponible en la respuesta de la búsqueda. */
    private void googleDevuelveUnoConFoto() {
        PlacesSearchResult.Place lugar = new PlacesSearchResult.Place(
            "ChIJ-nuevo", "Thunder Burger", "Costa Rica 5827", -34.5900, -58.4270,
            "places/ChIJ-nuevo/photos/una", "1200x900|Alguien",
            "hamburger_restaurant", Set.of("hamburger_restaurant"));
        when(placesClient.searchText(anyString(), any()))
            .thenReturn(new PlacesSearchResult(List.of(lugar), null));
        when(placesClient.downloadPhoto(anyString())).thenReturn(new byte[] {1, 2, 3});
    }

    /** Apagado: el local entra igual, pero sin portada y sin gastar la cuota de fotos. */
    @Test
    void apagadoTraeElLocalPeroNoBajaLaFoto() {
        googleDevuelveUnoConFoto();

        PlacesSyncReport report = service.sync(false);

        assertThat(report.created()).isEqualTo(1);
        assertThat(guardado().getPhotoUrl()).isNull();
        verify(placesClient, never()).downloadPhoto(anyString());
        verify(quotaGuard, never()).record(PlacesCallType.PHOTO);
    }

    /** Prendido sigue bajando, que es lo de siempre. */
    @Test
    void prendidoBajaLaFotoComoSiempre() {
        googleDevuelveUnoConFoto();

        PlacesSyncReport report = service.sync(true);

        assertThat(report.photosDownloaded()).isEqualTo(1);
        assertThat(guardado().getPhotoUrl()).isEqualTo("/api/place-photos/x.jpg");
    }

    /** Y sin argumento se comporta como antes: el cron no cambia de conducta. */
    @Test
    void sinDecirNadaBajaFotos() {
        googleDevuelveUnoConFoto();

        assertThat(service.sync().photosDownloaded()).isEqualTo(1);
    }

    /**
     * El segundo camino: completar la portada de un local que ya estaba guardado.
     *
     * Es el que más fotos gasta —son mil doscientos locales sin portada— y el que hay que
     * poder apagar para que el barrido no se lleve la cuota del mes de arrastre.
     */
    @Test
    void apagadoNoCompletaLasPortadasQueFaltan() {
        when(placesClient.searchText(anyString(), any()))
            .thenReturn(new PlacesSearchResult(List.of(), null));
        when(repository.findByPhotoUrlIsNull()).thenReturn(List.of(
            BurgerJoint.builder().id(1L).placeId("ChIJ-viejo").name("Sin portada")
                .address("Una dirección").area("Palermo").build()));

        service.sync(false);

        verify(placesClient, never()).fotosDe(anyString());
        verify(placesClient, never()).downloadPhoto(anyString());
    }

    /** Y el tercero: recambiar las que se eligieron con una regla vieja. */
    @Test
    void apagadoNoRecambiaLasFotosViejas() {
        when(placesClient.searchText(anyString(), any()))
            .thenReturn(new PlacesSearchResult(List.of(), null));
        when(repository.conFotoElegidaConUnaReglaVieja(anyInt())).thenReturn(List.of(
            BurgerJoint.builder().id(1L).placeId("ChIJ-viejo").name("Portada vieja")
                .address("Una dirección").area("Palermo")
                .photoUrl("/api/place-photos/vieja.jpg").photoRule(1).build()));

        service.sync(false);

        verify(placesClient, never()).fotosDe(anyString());
        verify(placesClient, never()).downloadPhoto(anyString());
    }

    private BurgerJoint guardado() {
        ArgumentCaptor<BurgerJoint> capturado = ArgumentCaptor.forClass(BurgerJoint.class);
        verify(repository).save(capturado.capture());
        return capturado.getValue();
    }
}
