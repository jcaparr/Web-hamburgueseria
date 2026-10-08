package com.hamburguesas.places;

import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.model.PlacesCallType;
import com.hamburguesas.repository.BurgerJointRepository;
import com.hamburguesas.repository.RatingRepository;
import com.hamburguesas.repository.SavedTourRepository;
import com.hamburguesas.repository.WishlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyInt;
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
    private PlacesProperties properties;

    @BeforeEach
    void setUp() {
        properties = new PlacesProperties();
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
        when(repository.conFotoElegidaConUnaReglaVieja(anyInt())).thenReturn(List.of());
        when(placesClient.searchText(anyString(), any())).thenReturn(unLugarConFoto());
        when(placesClient.downloadPhoto(anyString())).thenReturn(IMAGEN);
        when(photoStorage.save(anyString(), any())).thenReturn("/api/place-photos/abc.jpg");

        service = ServicioArmado.armar(
            properties, placesClient, quotaGuard, photoStorage, repository, new Zonas(new Barrios(), properties),
            mock(RatingRepository.class), mock(WishlistRepository.class),
            mock(SavedTourRepository.class),
            new FastFoodMarker(repository, properties));
    }

    private PlacesSearchResult unLugarConFoto() {
        return new PlacesSearchResult(
            List.of(new PlacesSearchResult.Place(
                "ChIJ123", "Thunder Burger", "Costa Rica 5827", -34.58, -58.43,
                "places/ChIJ123/photos/abc", "800x600|Un cliente", "hamburger_restaurant",
                java.util.Set.of("hamburger_restaurant"))),
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
        when(placesClient.fotosDe(eq("ChIJ-BK"), any())).thenReturn(java.util.List.of(new FotoElegida("places/ChIJ-BK/photos/abc", "huella-de-places/ChIJ-BK/photos/abc")));

        PlacesSyncReport report = service.sync();

        assertThat(invisible.getPhotoUrl()).isEqualTo("/api/place-photos/abc.jpg");
        assertThat(report.photosDownloaded()).isEqualTo(1);
        verify(quotaGuard).record(PlacesCallType.LISTA_DE_FOTOS);
        verify(quotaGuard).record(PlacesCallType.PHOTO);
    }

    /**
     * Cuántas fotos tiene un local en Google sale de su ficha, que se paga aparte. Si
     * después no quedaba cuota para bajar ninguna, el dato no se guardaba y el próximo
     * censo gastaba otra ficha en volver a preguntarlo (#98).
     */
    @Test
    void laCuentaDeFotosSeGuardaAunqueNoQuedeCuotaParaBajarlas() {
        when(placesClient.searchText(anyString(), any()))
            .thenReturn(new PlacesSearchResult(List.of(), null));
        when(quotaGuard.canCall(PlacesCallType.PHOTO)).thenReturn(false);

        BurgerJoint conFotos = BurgerJoint.builder()
            .id(9L).placeId("ChIJ-F").name("Con Fotos").address("Calle 1").area(AREA)
            .build();
        when(repository.findByPhotoUrlIsNull()).thenReturn(List.of(conFotos));
        when(placesClient.fotosDe(eq("ChIJ-F"), any())).thenReturn(List.of(
            new FotoElegida("places/ChIJ-F/photos/a", "huella-a"),
            new FotoElegida("places/ChIJ-F/photos/b", "huella-b")));

        service.sync();

        assertThat(conFotos.getPhotoUrl()).isNull();
        verify(repository).save(conFotos);
        assertThat(conFotos.getFotosEnGoogle()).isEqualTo(2);
        assertThat(conFotos.isSinFotosEnGoogle()).isFalse();
        verify(placesClient, never()).downloadPhoto(anyString());
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
        when(placesClient.fotosDe(eq("ChIJ-X"), any())).thenReturn(java.util.List.of());

        PlacesSyncReport report = service.sync();

        assertThat(sinFotos.getPhotoUrl()).isNull();
        assertThat(report.photosDownloaded()).isZero();
        verify(placesClient, never()).downloadPhoto(anyString());
    }

    /**
     * Una sucursal sin portada se queda con la foto de otra sucursal de la misma marca,
     * sin pedirle nada a Google. Es preferible el frente de otro local de la misma marca
     * antes que un recuadro con iniciales.
     */
    @Test
    void unaSucursalSinFotoUsaLaDeSuHermana() {
        when(placesClient.searchText(anyString(), any()))
            .thenReturn(new PlacesSearchResult(List.of(), null));
        properties.setFastFoodBrands(List.of("burgerking"));

        BurgerJoint conFoto = BurgerJoint.builder()
            .id(1L).placeId("ChIJ-BK1").name("Burger King").address("Corrientes 1").area(AREA)
            .photoUrl("/api/place-photos/burgerking.jpg")
            .build();
        BurgerJoint nueva = BurgerJoint.builder()
            .id(2L).placeId("ChIJ-BK2").name("Burger King").address("Cabildo 2").area(AREA)
            .build();

        when(repository.findByPhotoUrlIsNotNull()).thenReturn(List.of(conFoto));
        when(repository.findByPhotoUrlIsNull()).thenReturn(List.of(nueva));
        when(placesClient.fotosDe(eq("ChIJ-BK2"), any())).thenReturn(java.util.List.of());

        PlacesSyncReport report = service.sync();

        assertThat(nueva.getPhotoUrl()).isEqualTo("/api/place-photos/burgerking.jpg");
        assertThat(report.photosReused()).isEqualTo(1);
        // Prestarla no cuesta una llamada a Google, que es medio punto del asunto.
        assertThat(report.photosDownloaded()).isZero();
        verify(placesClient, never()).downloadPhoto(anyString());
        verify(placesClient, never()).fotosDe(anyString(), any());
    }

    /**
     * Llamarse igual no alcanza para prestar la foto (#99). "Big Burger" son tres locales
     * sin ninguna relación, en González Catán, Merlo y Pontevedra: el que se quede sin
     * portada no puede recibir la de otro, porque la tarjeta mostraría un local por otro.
     */
    @Test
    void unLocalQueSeLlamaIgualQueOtroSinSerMarcaNoRecibeSuFoto() {
        when(placesClient.searchText(anyString(), any()))
            .thenReturn(new PlacesSearchResult(List.of(), null));

        BurgerJoint deMerlo = BurgerJoint.builder()
            .id(1L).placeId("ChIJ-BB1").name("Big Burger").address("Merlo 1").area(AREA)
            .photoUrl("/api/place-photos/merlo.jpg")
            .build();
        BurgerJoint dePontevedra = BurgerJoint.builder()
            .id(2L).placeId("ChIJ-BB2").name("Big Burger").address("Pontevedra 2").area(AREA)
            .build();

        when(repository.findByPhotoUrlIsNotNull()).thenReturn(List.of(deMerlo));
        when(repository.findByPhotoUrlIsNull()).thenReturn(List.of(dePontevedra));
        when(placesClient.fotosDe(eq("ChIJ-BB2"), any())).thenReturn(List.of());

        PlacesSyncReport report = service.sync();

        assertThat(dePontevedra.getPhotoUrl()).isNull();
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
        when(placesClient.fotosDe(eq("ChIJ-B"), any())).thenReturn(java.util.List.of());

        PlacesSyncReport report = service.sync();

        assertThat(solitario.getPhotoUrl()).isNull();
        assertThat(report.photosReused()).isZero();
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
        when(repository.conFotoElegidaConUnaReglaVieja(anyInt())).thenReturn(List.of(vieja));
        when(placesClient.fotosDe(eq("ChIJ-V"), any())).thenReturn(java.util.List.of(new FotoElegida("places/ChIJ-V/photos/mejor", "huella-de-places/ChIJ-V/photos/mejor")));

        service.sync();

        assertThat(vieja.getPhotoUrl()).isEqualTo("/api/place-photos/abc.jpg");
        // Anotar cuál es la que tenemos es lo que evita revisarla de nuevo el mes que viene.
        assertThat(vieja.getPhotoName()).isEqualTo("places/ChIJ-V/photos/mejor");
    }

    /**
     * Si al revisar con la regla nueva gana la misma foto que ya tenemos, no se baja
     * de nuevo: sería pagarle a Google por el mismo archivo. Alcanza con anotar que ya
     * está revisada para no volver a mirarla el mes que viene.
     */
    @Test
    void siLaReglaNuevaEligeLaMismaFotoNoLaVuelveABajar() {
        when(placesClient.searchText(anyString(), any()))
            .thenReturn(new PlacesSearchResult(List.of(), null));

        BurgerJoint vieja = BurgerJoint.builder()
            .id(1L).placeId("ChIJ-V").name("Weiss Burger").address("Rivadavia 1").area(AREA)
            .photoUrl("/api/place-photos/vieja.jpg")
            .photoName("places/ChIJ-V/photos/la-misma")
            .photoFingerprint("huella-de-places/ChIJ-V/photos/la-misma")
            .photoRule(2)
            .build();
        when(repository.conFotoElegidaConUnaReglaVieja(anyInt())).thenReturn(List.of(vieja));
        when(placesClient.fotosDe(eq("ChIJ-V"), any())).thenReturn(java.util.List.of(new FotoElegida("places/ChIJ-V/photos/la-misma", "huella-de-places/ChIJ-V/photos/la-misma")));

        service.sync();

        verify(placesClient, never()).downloadPhoto(anyString());
        assertThat(vieja.getPhotoUrl()).isEqualTo("/api/place-photos/vieja.jpg");
        assertThat(vieja.getPhotoRule()).isEqualTo(EleccionDeFoto.REGLA_DE_FOTO);
    }

    /** Una foto con su nombre ya anotado no se vuelve a revisar: sería pagar de nuevo. */
    @Test
    void noRevisaLasQueYaTienenSuNombreAnotado() {
        when(placesClient.searchText(anyString(), any()))
            .thenReturn(new PlacesSearchResult(List.of(), null));
        when(repository.conFotoElegidaConUnaReglaVieja(anyInt())).thenReturn(List.of());

        service.sync();

        verify(placesClient, never()).fotosDe(anyString(), any());
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
