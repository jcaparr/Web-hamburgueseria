package com.hamburguesas.places;

import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.model.PlacesCallType;
import com.hamburguesas.repository.BurgerJointRepository;
import com.hamburguesas.repository.RatingRepository;
import com.hamburguesas.repository.SavedTourRepository;
import com.hamburguesas.repository.WishlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Preguntar qué fotos tiene un local no se paga, y las fotos pagas tienen techo (#199).
 *
 * En octubre de 2026 las 5.000 fichas gratis del mes se fueron en preguntar qué fotos
 * tenía cada local, porque la consulta pedía también el nombre, que Google cobra como Pro.
 * Y quedaron 302 locales sin portada con la cuota de fotos gastada: completarlos salía
 * unos dos dólares, y se decidió pagarlos en vez de esperar a noviembre.
 *
 * No sale a internet: el cliente de Google está simulado.
 */
class FotosSinGastarFichasTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final byte[] IMAGEN = new byte[] { 1, 2, 3 };
    private static final int TRAMO_GRATUITO = 1000;

    /**
     * Los campos del tramo "Essentials IDs Only" de la ficha, que Google no cobra.
     * https://developers.google.com/maps/documentation/places/web-service/place-details
     */
    private static final Set<String> CAMPOS_GRATIS = Set.of(
        "attributions", "consumerAlert", "id", "movedPlace", "movedPlaceId", "name", "photos");

    private PlacesClient placesClient;
    private PlacesQuotaGuard quotaGuard;
    private BurgerJointRepository repository;
    private PlacesSyncService service;

    /** Las fotos bajadas en el mes, como el contador de la base. */
    private AtomicInteger fotosDelMes;

    @BeforeEach
    void setUp() {
        PlacesProperties properties = new PlacesProperties();
        properties.setApiKey("clave-de-prueba");
        properties.getSync().setDelayBetweenCallsMs(0);

        placesClient = mock(PlacesClient.class);
        quotaGuard = mock(PlacesQuotaGuard.class);
        repository = mock(BurgerJointRepository.class);
        PhotoStorage photoStorage = mock(PhotoStorage.class);

        // El tramo gratuito de fotos ya se gastó entero, como en octubre.
        fotosDelMes = new AtomicInteger(TRAMO_GRATUITO);
        when(quotaGuard.canCall(any())).thenReturn(true);
        when(quotaGuard.canCall(PlacesCallType.PHOTO))
            .thenAnswer(i -> fotosDelMes.get() < TRAMO_GRATUITO);
        when(quotaGuard.used(PlacesCallType.PHOTO)).thenAnswer(i -> fotosDelMes.get());
        when(quotaGuard.limitFor(PlacesCallType.PHOTO)).thenReturn(TRAMO_GRATUITO);
        doAnswer(i -> fotosDelMes.incrementAndGet()).when(quotaGuard).record(PlacesCallType.PHOTO);

        when(repository.findByPhotoUrlIsNull()).thenReturn(List.of());
        when(repository.findByPhotoUrlIsNotNull()).thenReturn(List.of());
        when(repository.conFotoElegidaConUnaReglaVieja(anyInt())).thenReturn(List.of());
        when(repository.nombresDeRubro(anyString())).thenReturn(List.of());
        when(placesClient.downloadPhoto(anyString())).thenReturn(IMAGEN);
        when(photoStorage.save(anyString(), any())).thenReturn("/api/place-photos/nueva.jpg");

        service = ServicioArmado.armar(
            properties, placesClient, quotaGuard, photoStorage, repository, new Zonas(new Barrios(), properties),
            mock(RatingRepository.class), mock(WishlistRepository.class),
            mock(SavedTourRepository.class), new FastFoodMarker(repository, properties));
    }

    private static BurgerJoint sinPortada(long id) {
        return BurgerJoint.builder()
            .id(id).placeId("ChIJ-" + id).name("Hamburguesería " + id)
            .address("Una dirección").area("Palermo")
            .build();
    }

    private static List<FotoElegida> unaFoto(String placeId) {
        return List.of(new FotoElegida("places/" + placeId + "/photos/a", "huella-" + placeId));
    }

    /** Con un solo campo de otro tramo, Google cobra la llamada entera a ese precio. */
    @Test
    void laConsultaDeFotosSoloPideCamposGratis() {
        assertThat(Arrays.asList(PlacesClient.CAMPOS_DE_LAS_FOTOS.split(",")))
            .isNotEmpty()
            .allSatisfy(campo -> assertThat(CAMPOS_GRATIS).contains(campo.trim()));
    }

    /**
     * Sin pedirle el nombre a Google, la foto que subió el local se sigue reconociendo:
     * el nombre sale de lo guardado.
     */
    @Test
    void reconoceLaFotoDelLocalConElNombreGuardado() throws Exception {
        JsonNode sinNombre = MAPPER.readTree("""
            {"id": "ChIJ-1", "photos": [
              {"name": "places/ChIJ-1/photos/cliente", "widthPx": 1600, "heightPx": 1200,
               "authorAttributions": [{"displayName": "Un cliente"}]},
              {"name": "places/ChIJ-1/photos/local", "widthPx": 1600, "heightPx": 1200,
               "authorAttributions": [{"displayName": "Thunder Burger Palermo"}]}
            ]}
            """);

        List<FotoElegida> fotos = EleccionDeFoto.mejoresFotos(sinNombre, "Thunder Burger", null);

        assertThat(fotos).first()
            .extracting(FotoElegida::name).isEqualTo("places/ChIJ-1/photos/local");
    }

    @Test
    void preguntarCuentaComoListaDeFotosConElNombreGuardado() {
        BurgerJoint local = sinPortada(1);
        when(repository.findByPhotoUrlIsNull()).thenReturn(List.of(local));
        when(placesClient.fotosDe("ChIJ-1", "Hamburguesería 1")).thenReturn(List.of());

        service.revisarFotos();

        verify(placesClient).fotosDe("ChIJ-1", "Hamburguesería 1");
        verify(quotaGuard).record(PlacesCallType.LISTA_DE_FOTOS);
    }

    /** Sin pedirlo explícitamente, el tramo gratuito sigue siendo el techo. */
    @Test
    void sinPagasNoSeBajaNadaPasadoElTramo() {
        BurgerJoint local = sinPortada(1);
        when(repository.findByPhotoUrlIsNull()).thenReturn(List.of(local));
        when(placesClient.fotosDe(anyString(), any())).thenReturn(unaFoto("ChIJ-1"));

        service.revisarFotos();

        assertThat(local.getPhotoUrl()).isNull();
        verify(placesClient, never()).downloadPhoto(anyString());
    }

    @Test
    void lasPagasAlcanzanParaLasQueSeAutorizaron() {
        List<BurgerJoint> locales = List.of(sinPortada(1), sinPortada(2), sinPortada(3));
        when(repository.findByPhotoUrlIsNull()).thenReturn(locales);
        when(placesClient.fotosDe(anyString(), any()))
            .thenAnswer(i -> unaFoto(i.getArgument(0)));

        PlacesSyncReport reporte = service.revisarFotos(2);

        assertThat(reporte.photosDownloaded()).isEqualTo(2);
        assertThat(locales).filteredOn(l -> l.getPhotoUrl() != null).hasSize(2);
        assertThat(fotosDelMes.get()).isEqualTo(TRAMO_GRATUITO + 2);
    }

    /**
     * Pedirlas otra vez no las duplica: cuentan sobre el mismo contador del mes. Y se
     * recortan al techo del código por más que se pidan miles.
     */
    @Test
    void enUnMesNoSePaganMasQueElTecho() {
        fotosDelMes.set(TRAMO_GRATUITO + PlacesSyncService.FOTOS_PAGAS_POR_MES);
        BurgerJoint local = sinPortada(1);
        when(repository.findByPhotoUrlIsNull()).thenReturn(List.of(local));
        when(placesClient.fotosDe(anyString(), any())).thenReturn(unaFoto("ChIJ-1"));

        service.revisarFotos(10_000);

        assertThat(local.getPhotoUrl()).isNull();
        verify(placesClient, never()).downloadPhoto(anyString());
    }

    /** Un local que ya tiene portada puede esperar al tramo gratuito del mes que viene. */
    @Test
    void recambiarUnaFotoQueYaEstaNuncaUsaLasPagas() {
        BurgerJoint conFotoVieja = BurgerJoint.builder()
            .id(7L).placeId("ChIJ-7").name("Hamburguesería 7")
            .address("Una dirección").area("Palermo")
            .photoUrl("/api/place-photos/vieja.jpg").photoName("places/ChIJ-7/photos/vieja")
            .photoFingerprint("huella-vieja").photoRule(1)
            .build();
        when(repository.conFotoElegidaConUnaReglaVieja(anyInt())).thenReturn(List.of(conFotoVieja));
        when(placesClient.fotosDe(anyString(), any())).thenReturn(unaFoto("ChIJ-7"));

        service.revisarFotos(5);

        assertThat(conFotoVieja.getPhotoUrl()).isEqualTo("/api/place-photos/vieja.jpg");
        verify(placesClient, never()).downloadPhoto(anyString());
    }

    /** Con el tramo gratuito sin gastar, las pagas no se tocan: se baja como siempre. */
    @Test
    void primeroSeUsaElTramoGratuito() {
        fotosDelMes.set(TRAMO_GRATUITO - 1);
        List<BurgerJoint> locales = List.of(sinPortada(1), sinPortada(2));
        when(repository.findByPhotoUrlIsNull()).thenReturn(locales);
        when(placesClient.fotosDe(anyString(), any()))
            .thenAnswer(i -> unaFoto(i.getArgument(0)));

        service.revisarFotos(1);

        verify(placesClient, times(2)).downloadPhoto(anyString());
        assertThat(fotosDelMes.get()).isEqualTo(TRAMO_GRATUITO + 1);
    }
}
