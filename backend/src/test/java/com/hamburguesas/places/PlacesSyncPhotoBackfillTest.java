package com.hamburguesas.places;

import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.model.PlacesCallType;
import com.hamburguesas.repository.BurgerJointRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cubre que la sincronización complete las fotos que faltan.
 *
 * Los 684 locales de la base se cargaron sin foto: Google las omitía porque el
 * proyecto no tenía facturación, y además solo se pedían al crear el local, así que
 * nadie volvía a intentarlo. Sin esto, habilitar la facturación no habría cambiado
 * nada para los locales que ya existían.
 *
 * No sale a internet: el cliente de Google está simulado.
 */
class PlacesSyncPhotoBackfillTest {

    private static final String AREA = "Palermo";
    private static final byte[] IMAGEN = new byte[] { 1, 2, 3 };

    private PlacesClient placesClient;
    private PlacesQuotaGuard quotaGuard;
    private PhotoStorage photoStorage;
    private BurgerJointRepository repository;
    private PlacesSyncService service;

    @BeforeEach
    void setUp() {
        PlacesProperties properties = new PlacesProperties();
        properties.setApiKey("clave-de-prueba");
        properties.getSync().setAreas(List.of(AREA));
        properties.getSync().setMaxPagesPerArea(1);
        properties.getSync().setDelayBetweenCallsMs(0);

        placesClient = mock(PlacesClient.class);
        quotaGuard = mock(PlacesQuotaGuard.class);
        photoStorage = mock(PhotoStorage.class);
        repository = mock(BurgerJointRepository.class);

        when(quotaGuard.canCall(any())).thenReturn(true);
        when(placesClient.searchText(anyString(), any())).thenReturn(unLugarConFoto());
        when(placesClient.downloadPhoto(anyString())).thenReturn(IMAGEN);
        when(photoStorage.save(anyString(), any())).thenReturn("/api/place-photos/abc.jpg");

        service = new PlacesSyncService(properties, placesClient, quotaGuard, photoStorage, repository);
    }

    private PlacesSearchResult unLugarConFoto() {
        return new PlacesSearchResult(
            List.of(new PlacesSearchResult.Place(
                "ChIJ123", "Thunder Burger", "Costa Rica 5827", -34.58, -58.43,
                "places/ChIJ123/photos/abc")),
            null);
    }

    @Test
    void unLocalYaCargadoSinFotoLaRecibe() {
        BurgerJoint sinFoto = BurgerJoint.builder()
            .id(1L).placeId("ChIJ123").name("Thunder Burger").address("Costa Rica 5827").area(AREA)
            .build();
        when(repository.findByPlaceId("ChIJ123")).thenReturn(Optional.of(sinFoto));

        PlacesSyncReport report = service.sync();

        assertThat(sinFoto.getPhotoUrl()).isEqualTo("/api/place-photos/abc.jpg");
        assertThat(report.photosDownloaded()).isEqualTo(1);
        assertThat(report.updated()).isEqualTo(1);
        verify(quotaGuard).record(PlacesCallType.PHOTO);
    }

    /** Volver a bajar una foto que ya tenemos sería pagarle dos veces a Google por lo mismo. */
    @Test
    void unLocalQueYaTieneFotoNoLaVuelveABajar() {
        BurgerJoint conFoto = BurgerJoint.builder()
            .id(1L).placeId("ChIJ123").name("Thunder Burger").address("Costa Rica 5827").area(AREA)
            .photoUrl("/api/place-photos/vieja.jpg")
            .build();
        when(repository.findByPlaceId("ChIJ123")).thenReturn(Optional.of(conFoto));

        PlacesSyncReport report = service.sync();

        assertThat(conFoto.getPhotoUrl()).isEqualTo("/api/place-photos/vieja.jpg");
        assertThat(report.photosDownloaded()).isZero();
        verify(placesClient, never()).downloadPhoto(anyString());
    }

    /** Con el tope mensual agotado la sincronización sigue, pero sin bajar fotos. */
    @Test
    void conLaCuotaDeFotosAgotadaActualizaIgualPeroNoBaja() {
        when(quotaGuard.canCall(PlacesCallType.PHOTO)).thenReturn(false);
        BurgerJoint sinFoto = BurgerJoint.builder()
            .id(1L).placeId("ChIJ123").name("Thunder Burger").address("Costa Rica 5827").area(AREA)
            .build();
        when(repository.findByPlaceId("ChIJ123")).thenReturn(Optional.of(sinFoto));

        PlacesSyncReport report = service.sync();

        assertThat(sinFoto.getPhotoUrl()).isNull();
        assertThat(report.updated()).isEqualTo(1);
        assertThat(report.photosDownloaded()).isZero();
        verify(placesClient, never()).downloadPhoto(anyString());
    }
}
