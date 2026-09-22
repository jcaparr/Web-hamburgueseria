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
        when(repository.findByPhotoUrlIsNull()).thenReturn(List.of());
        when(repository.findByPhotoUrlIsNotNullAndPhotoNameIsNull()).thenReturn(List.of());
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

    /**
     * Los locales que ninguna búsqueda devuelve —Burger King, por ejemplo, que Google
     * clasifica como comida rápida y el filtro estricto deja afuera— se quedaban sin
     * foto para siempre, porque la foto solo llegaba a través de una búsqueda.
     */
    @Test
    void lesConsigueFotoALosQueNingunaBusquedaDevuelve() {
        when(placesClient.searchText(anyString(), any()))
            .thenReturn(new PlacesSearchResult(List.of(), null));

        BurgerJoint invisible = BurgerJoint.builder()
            .id(9L).placeId("ChIJ-BK").name("Burger King").address("Av. Corrientes 1").area(AREA)
            .build();
        when(repository.findByPhotoUrlIsNull()).thenReturn(List.of(invisible));
        when(placesClient.photoNameFor("ChIJ-BK")).thenReturn("places/ChIJ-BK/photos/abc");

        PlacesSyncReport report = service.sync();

        assertThat(invisible.getPhotoUrl()).isEqualTo("/api/place-photos/abc.jpg");
        assertThat(report.photosDownloaded()).isEqualTo(1);
        verify(quotaGuard).record(PlacesCallType.DETAILS);
        verify(quotaGuard).record(PlacesCallType.PHOTO);
    }

    /** Que Google no tenga fotos de un local es normal: no se marca nada y se reintenta. */
    @Test
    void unLocalSinFotosEnGoogleQuedaComoEstaba() {
        when(placesClient.searchText(anyString(), any()))
            .thenReturn(new PlacesSearchResult(List.of(), null));

        BurgerJoint sinFotos = BurgerJoint.builder()
            .id(9L).placeId("ChIJ-X").name("Sin Fotos").address("Calle 1").area(AREA)
            .build();
        when(repository.findByPhotoUrlIsNull()).thenReturn(List.of(sinFotos));
        when(placesClient.photoNameFor("ChIJ-X")).thenReturn(null);

        PlacesSyncReport report = service.sync();

        assertThat(sinFotos.getPhotoUrl()).isNull();
        assertThat(report.photosDownloaded()).isZero();
        verify(placesClient, never()).downloadPhoto(anyString());
    }

    /**
     * Una sucursal cuya dirección Google no tiene fotografiada se queda con la foto de
     * otra sucursal de la misma cadena. Es preferible el frente de otro local de la
     * misma marca antes que un recuadro con iniciales.
     */
    @Test
    void unaSucursalSinFotoUsaLaDeSuHermana() {
        when(placesClient.searchText(anyString(), any()))
            .thenReturn(new PlacesSearchResult(List.of(), null));

        BurgerJoint conFoto = BurgerJoint.builder()
            .id(1L).placeId("ChIJ-BK1").name("Burger King").address("Corrientes 1").area(AREA)
            .photoUrl("/api/place-photos/burgerking.jpg")
            .build();
        BurgerJoint nueva = BurgerJoint.builder()
            .id(2L).placeId("ChIJ-BK2").name("Burger King").address("Cabildo 2").area(AREA)
            .build();

        when(repository.findByPhotoUrlIsNotNull()).thenReturn(List.of(conFoto));
        when(repository.findByPhotoUrlIsNull()).thenReturn(List.of(nueva));
        when(placesClient.photoNameFor("ChIJ-BK2")).thenReturn(null);

        PlacesSyncReport report = service.sync();

        assertThat(nueva.getPhotoUrl()).isEqualTo("/api/place-photos/burgerking.jpg");
        assertThat(report.photosReused()).isEqualTo(1);
        // Prestarla no cuesta una llamada a Google, que es medio punto del asunto.
        assertThat(report.photosDownloaded()).isZero();
        verify(placesClient, never()).downloadPhoto(anyString());
    }

    /** La foto propia de la sucursal siempre es mejor que la prestada. */
    @Test
    void siGoogleTieneLaFotoDeEsaSucursalGanaSobreLaPrestada() {
        when(placesClient.searchText(anyString(), any()))
            .thenReturn(new PlacesSearchResult(List.of(), null));

        BurgerJoint hermana = BurgerJoint.builder()
            .id(1L).placeId("ChIJ-BK1").name("Burger King").address("Corrientes 1").area(AREA)
            .photoUrl("/api/place-photos/hermana.jpg")
            .build();
        BurgerJoint nueva = BurgerJoint.builder()
            .id(2L).placeId("ChIJ-BK2").name("Burger King").address("Cabildo 2").area(AREA)
            .build();

        when(repository.findByPhotoUrlIsNotNull()).thenReturn(List.of(hermana));
        when(repository.findByPhotoUrlIsNull()).thenReturn(List.of(nueva));
        when(placesClient.photoNameFor("ChIJ-BK2")).thenReturn("places/ChIJ-BK2/photos/propia");

        PlacesSyncReport report = service.sync();

        assertThat(nueva.getPhotoUrl()).isEqualTo("/api/place-photos/abc.jpg");
        assertThat(report.photosDownloaded()).isEqualTo(1);
        assertThat(report.photosReused()).isZero();
    }

    /** Sin una cadena en común no se presta nada: son dos locales distintos. */
    @Test
    void noLePrestaLaFotoAUnLocalDeOtroNombre() {
        when(placesClient.searchText(anyString(), any()))
            .thenReturn(new PlacesSearchResult(List.of(), null));

        BurgerJoint otro = BurgerJoint.builder()
            .id(1L).placeId("ChIJ-A").name("Thunder Burger").address("Costa Rica 1").area(AREA)
            .photoUrl("/api/place-photos/thunder.jpg")
            .build();
        BurgerJoint solitario = BurgerJoint.builder()
            .id(2L).placeId("ChIJ-B").name("Heaven").address("Alsina 2").area(AREA)
            .build();

        when(repository.findByPhotoUrlIsNotNull()).thenReturn(List.of(otro));
        when(repository.findByPhotoUrlIsNull()).thenReturn(List.of(solitario));
        when(placesClient.photoNameFor("ChIJ-B")).thenReturn(null);

        PlacesSyncReport report = service.sync();

        assertThat(solitario.getPhotoUrl()).isNull();
        assertThat(report.photosReused()).isZero();
    }

    @Test
    void reconoceLaCadenaAunqueElNombreTraigaElBarrioOAcentos() {
        assertThat(PlacesSyncService.chainKey("Dean & Dennys - Palermo Soho"))
            .isEqualTo(PlacesSyncService.chainKey("Dean & Dennys - Barrio Norte"));
        assertThat(PlacesSyncService.chainKey("Chopi's Burger"))
            .isEqualTo(PlacesSyncService.chainKey("CHOPI'S  BURGER"));
        assertThat(PlacesSyncService.chainKey("Ché Burgers"))
            .isEqualTo(PlacesSyncService.chainKey("che burgers"));
        // Parecerse no alcanza: si alcanzara, un "Heaven" cualquiera heredaría fotos ajenas.
        assertThat(PlacesSyncService.chainKey("Burger King"))
            .isNotEqualTo(PlacesSyncService.chainKey("Burger King Express"));
    }

    /**
     * Las fotos bajadas con la regla de elección vieja se revisan una vez. Sobre 150
     * locales reales, la regla nueva elige otra foto en 47: casi siempre una apaisada
     * en lugar de una vertical, que en las tarjetas se veía recortada al medio.
     */
    @Test
    void revisaUnaVezLasFotosBajadasConLaReglaVieja() {
        when(placesClient.searchText(anyString(), any()))
            .thenReturn(new PlacesSearchResult(List.of(), null));

        BurgerJoint vieja = BurgerJoint.builder()
            .id(1L).placeId("ChIJ-V").name("Weiss Burger").address("Rivadavia 1").area(AREA)
            .photoUrl("/api/place-photos/vieja.jpg")
            .build();
        when(repository.findByPhotoUrlIsNotNullAndPhotoNameIsNull()).thenReturn(List.of(vieja));
        when(placesClient.photoNameFor("ChIJ-V")).thenReturn("places/ChIJ-V/photos/mejor");

        service.sync();

        assertThat(vieja.getPhotoUrl()).isEqualTo("/api/place-photos/abc.jpg");
        // Anotar cuál es la que tenemos es lo que evita revisarla de nuevo el mes que viene.
        assertThat(vieja.getPhotoName()).isEqualTo("places/ChIJ-V/photos/mejor");
    }

    /** Una foto con su nombre ya anotado no se vuelve a revisar: sería pagar de nuevo. */
    @Test
    void noRevisaLasQueYaTienenSuNombreAnotado() {
        when(placesClient.searchText(anyString(), any()))
            .thenReturn(new PlacesSearchResult(List.of(), null));
        when(repository.findByPhotoUrlIsNotNullAndPhotoNameIsNull()).thenReturn(List.of());

        service.sync();

        verify(placesClient, never()).photoNameFor(anyString());
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
