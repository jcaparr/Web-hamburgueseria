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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Lo que usan las skills de agregar un local y de barrer una zona (#224, #225): agregar
 * el local que se revisó y no otro, ponerle la portada a uno solo, y barrer sin guardar
 * para revisar antes.
 */
class AgregarYRevisarTest {

    private static final String MAR_DEL_PLATA =
        "Av. Constitución 4205, B7600 Mar del Plata, Provincia de Buenos Aires, Argentina";

    private PlacesProperties properties;
    private PlacesClient placesClient;
    private BurgerJointRepository repository;
    private PlacesQuotaGuard quotaGuard;
    private PhotoStorage photoStorage;
    private PlacesSyncService service;

    @BeforeEach
    void setUp() {
        properties = new PlacesProperties();
        properties.setApiKey("clave-de-prueba");
        properties.getSync().setMaxPagesPerArea(1);
        properties.getSync().setQueryTemplates(List.of("hamburguesería en {barrio}"));
        properties.getSync().setDelayBetweenCallsMs(0);

        placesClient = mock(PlacesClient.class);
        repository = mock(BurgerJointRepository.class);
        quotaGuard = mock(PlacesQuotaGuard.class);
        photoStorage = mock(PhotoStorage.class);

        when(quotaGuard.canCall(any())).thenReturn(true);
        when(quotaGuard.limitFor(PlacesCallType.PHOTO)).thenReturn(1000);
        when(repository.findByPlaceId(anyString())).thenReturn(Optional.empty());
        when(repository.findByFastFoodFalse()).thenReturn(List.of());
        when(repository.nombresDeRubro(anyString())).thenReturn(List.of());
        when(repository.conFotoElegidaConUnaReglaVieja(anyInt())).thenReturn(List.of());

        service = ServicioArmado.armar(
            properties, placesClient, quotaGuard, photoStorage, repository,
            new Zonas(new Barrios(), properties), mock(RatingRepository.class),
            mock(WishlistRepository.class), mock(SavedTourRepository.class),
            new FastFoodMarker(repository, properties));
    }

    private static PlacesSearchResult.Place lugar(String id, String nombre, String direccion,
                                                 double lat, double lon, String rubro) {
        return new PlacesSearchResult.Place(id, nombre, direccion, lat, lon,
            null, null, rubro, Set.of(rubro));
    }

    // ---- agregar el que se revisó ----

    /**
     * Entre mostrar qué local devuelve Google y agregarlo pasan dos búsquedas. Si la
     * segunda trae otro, no se guarda nada: agregar uno por otro es peor que no agregar.
     */
    @Test
    void siGoogleDevuelveOtroLocalNoAgregaNada() {
        when(placesClient.searchText(anyString(), any(), eq(false))).thenReturn(new PlacesSearchResult(
            List.of(lugar("ChIJ-otro", "Otro local", "Costa Rica 5827, CABA", -34.59, -58.427, "restaurant")),
            null));

        LocalAgregado resultado = service.agregar("Ácido, Charlone 999", "ChIJ-acido");

        assertThat(resultado.resultado()).contains("otro local");
        verify(repository, never()).save(any(BurgerJoint.class));
    }

    @Test
    void siEsElQueSeRevisoLoAgrega() {
        when(placesClient.searchText(anyString(), any(), eq(false))).thenReturn(new PlacesSearchResult(
            List.of(lugar("ChIJ-acido", "Ácido", "Charlone 999, CABA", -34.5807, -58.4578, "restaurant")),
            null));

        assertThat(service.agregar("Ácido, Charlone 999", "ChIJ-acido").resultado()).isEqualTo("Agregado");
    }

    // ---- la portada de un solo local ----

    private BurgerJoint guardadoSinFoto() {
        BurgerJoint joint = BurgerJoint.builder().id(3000L).placeId("ChIJ-acido").name("Ácido")
            .address("Charlone 999").area("Chacarita").build();
        when(repository.findByPlaceId("ChIJ-acido")).thenReturn(Optional.of(joint));
        when(placesClient.fotosDe(eq("ChIJ-acido"), any())).thenReturn(List.of(
            new FotoElegida("places/x/photos/cocina", "3600x4800|gabriella villaça")));
        when(placesClient.downloadPhoto(anyString())).thenReturn(new byte[] {1, 2, 3});
        when(photoStorage.save(anyString(), any())).thenReturn("/api/place-photos/ChIJ-acido.jpg");
        return joint;
    }

    @Test
    void conFotosGratisLaPoneSinPagar() {
        BurgerJoint joint = guardadoSinFoto();

        FotoDeUnLocal resultado = service.portadaDeUnLocal("ChIJ-acido", false);

        assertThat(resultado.resultado()).isEqualTo("Puesta");
        assertThat(resultado.paga()).isFalse();
        assertThat(joint.getPhotoFingerprint()).isEqualTo("3600x4800|gabriella villaça");
    }

    /** Sin las gratis y sin permiso de pagar, no baja nada: el presupuesto es cero. */
    @Test
    void sinFotosGratisYSinPermisoNoPaga() {
        guardadoSinFoto();
        when(quotaGuard.canCall(PlacesCallType.PHOTO)).thenReturn(false);
        when(quotaGuard.used(PlacesCallType.PHOTO)).thenReturn(1314);

        FotoDeUnLocal resultado = service.portadaDeUnLocal("ChIJ-acido", false);

        assertThat(resultado.resultado()).contains("no se autorizó pagar");
        verify(placesClient, never()).downloadPhoto(anyString());
    }

    /**
     * Con permiso, paga una. Ya se habían pagado 314 este mes: el permiso tiene que
     * contarlas, porque se mide sobre el mes. Pedir "una paga" a secas no alcanzaba.
     */
    @Test
    void conPermisoPagaUnaAunqueYaSeHayanPagadoOtrasEsteMes() {
        guardadoSinFoto();
        when(quotaGuard.canCall(PlacesCallType.PHOTO)).thenReturn(false);
        when(quotaGuard.used(PlacesCallType.PHOTO)).thenReturn(1314);

        FotoDeUnLocal resultado = service.portadaDeUnLocal("ChIJ-acido", true);

        assertThat(resultado.resultado()).isEqualTo("Puesta, paga");
        assertThat(resultado.paga()).isTrue();
    }

    /** Llegado el tope de pagas del mes, ni con permiso. */
    @Test
    void conElTopeDePagasAlcanzadoNoPagaNiConPermiso() {
        guardadoSinFoto();
        when(quotaGuard.canCall(PlacesCallType.PHOTO)).thenReturn(false);
        when(quotaGuard.used(PlacesCallType.PHOTO)).thenReturn(1000 + PlacesSyncService.FOTOS_PAGAS_POR_MES);

        service.portadaDeUnLocal("ChIJ-acido", true);

        verify(placesClient, never()).downloadPhoto(anyString());
    }

    @Test
    void siYaTieneEsaFotoNoLaVuelveABajar() {
        BurgerJoint joint = guardadoSinFoto();
        joint.setPhotoFingerprint("3600x4800|gabriella villaça");

        assertThat(service.portadaDeUnLocal("ChIJ-acido", true).resultado()).contains("Ya tenía");
        verify(placesClient, never()).downloadPhoto(anyString());
    }

    // ---- barrer para revisar ----

    private void googleDevuelve(PlacesSearchResult.Place... lugares) {
        when(placesClient.searchText(anyString(), any()))
            .thenReturn(new PlacesSearchResult(List.of(lugares), null));
    }

    /**
     * Trae solo los que no están, con lo que hace falta para revisarlos, y no guarda nada:
     * los que se aprueban se agregan después, uno por uno.
     */
    @Test
    void barreSinGuardarYTraeLoQueHaceFaltaParaRevisar() {
        googleDevuelve(
            lugar("ChIJ-nuevo", "Burger del Puerto", MAR_DEL_PLATA, -37.9694, -57.5455, "hamburger_restaurant"),
            lugar("ChIJ-esta", "The Burger Company", "Honduras 4733, CABA, Argentina", -34.59, -58.427, "hamburger_restaurant"),
            lugar("ChIJ-colombia", "Versalles Burguer", "Cra. 8, Floridablanca, Colombia", 7.06, -73.09, "hamburger_restaurant"));
        when(repository.findByPlaceId("ChIJ-esta")).thenReturn(Optional.of(BurgerJoint.builder().build()));
        when(placesClient.revisionDe("ChIJ-nuevo"))
            .thenReturn(new RevisionDeGoogle(10, 523, 4.6, "Smash burgers jugosas y papas"));

        CandidatosDeZona resultado = service.candidatosDeZona("Mar del Plata, Buenos Aires", null);

        assertThat(resultado.candidatos()).singleElement().satisfies(c -> {
            assertThat(c.nombre()).isEqualTo("Burger del Puerto");
            assertThat(c.zona()).isEqualTo("Mar del Plata");
            assertThat(c.fotos()).isEqualTo(10);
            assertThat(c.opiniones()).isEqualTo(523);
            assertThat(c.puntaje()).isEqualTo(4.6);
            assertThat(c.resumen()).contains("Smash");
            assertThat(c.loAceptaria()).isTrue();
            assertThat(c.mapa()).contains("ChIJ-nuevo");
        });
        assertThat(resultado.yaEstaban()).isEqualTo(1);
        assertThat(resultado.fueraDeLaZona()).isEqualTo(1);
        assertThat(resultado.busquedas()).isEqualTo(1);
        verify(repository, never()).save(any(BurgerJoint.class));
    }

    /** Sin cuota de resúmenes los trae igual, sin esos datos, y lo avisa. */
    @Test
    void sinCuotaDeResumenesLosTraeSinDatosYAvisa() {
        googleDevuelve(lugar("ChIJ-nuevo", "Burger del Puerto", MAR_DEL_PLATA, -37.9694, -57.5455, "hamburger_restaurant"));
        when(quotaGuard.canCall(PlacesCallType.RESUMEN)).thenReturn(false);

        CandidatosDeZona resultado = service.candidatosDeZona("Mar del Plata, Buenos Aires", null);

        assertThat(resultado.candidatos()).singleElement().satisfies(c -> assertThat(c.opiniones()).isNull());
        assertThat(resultado.aviso()).contains("resúmenes");
        verify(placesClient, never()).revisionDe(anyString());
    }

    /** El mismo local en dos formas de preguntar aparece una sola vez, y se revisa una vez. */
    @Test
    void elMismoLocalEnDosBusquedasSeRevisaUnaVez() {
        properties.getSync().setQueryTemplates(List.of("hamburguesería en {barrio}", "burgers en {barrio}"));
        googleDevuelve(lugar("ChIJ-nuevo", "Burger del Puerto", MAR_DEL_PLATA, -37.9694, -57.5455, "hamburger_restaurant"));
        when(placesClient.revisionDe("ChIJ-nuevo")).thenReturn(new RevisionDeGoogle(3, 12, 4.0, null));

        CandidatosDeZona resultado = service.candidatosDeZona("Mar del Plata, Buenos Aires", null);

        assertThat(resultado.candidatos()).hasSize(1);
        assertThat(resultado.busquedas()).isEqualTo(2);
        verify(placesClient, org.mockito.Mockito.times(1)).revisionDe("ChIJ-nuevo");
    }
}
