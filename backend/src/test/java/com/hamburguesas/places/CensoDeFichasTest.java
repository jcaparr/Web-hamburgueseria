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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * La mitad barata de la revisión de fotos: preguntar sin bajar.
 *
 * Las dos cuotas son muy desparejas —cuatro mil fichas por mes contra mil fotos— y con
 * mil doscientos locales sin portada, bajar mientras se pregunta gasta la chica entera
 * antes de saber a quién vale la pena gastársela. Lo que se prueba acá es justamente que
 * no baje nada.
 */
class CensoDeFichasTest {

    private PlacesClient placesClient;
    private BurgerJointRepository repository;
    private PhotoStorage photoStorage;
    private PlacesQuotaGuard quotaGuard;
    private PlacesSyncService service;

    @BeforeEach
    void setUp() {
        PlacesProperties properties = new PlacesProperties();
        properties.setApiKey("clave-de-prueba");
        properties.getSync().setDelayBetweenCallsMs(0);

        placesClient = mock(PlacesClient.class);
        repository = mock(BurgerJointRepository.class);
        photoStorage = mock(PhotoStorage.class);
        quotaGuard = mock(PlacesQuotaGuard.class);

        when(quotaGuard.canCall(any())).thenReturn(true);
        when(repository.findByPhotoUrlIsNull()).thenReturn(List.of());
        when(repository.findByPhotoUrlIsNotNull()).thenReturn(List.of());
        when(repository.conFotoElegidaConUnaReglaVieja(anyInt())).thenReturn(List.of());
        when(repository.findByPlaceId(anyString())).thenReturn(Optional.empty());
        when(repository.nombresDeRubro(anyString())).thenReturn(List.of());

        service = new PlacesSyncService(
            properties, placesClient, quotaGuard, photoStorage, repository, new Barrios(),
            new Zonas(new Barrios(), properties), mock(RatingRepository.class),
            mock(WishlistRepository.class), mock(SavedTourRepository.class),
            new FastFoodMarker(repository, properties));
    }

    private BurgerJoint sinFoto(long id, String nombre) {
        return BurgerJoint.builder()
            .id(id).placeId("ChIJ" + id).name(nombre)
            .address("Una dirección").area("Quilmes")
            .build();
    }

    private static final List<FotoElegida> TIENE_FOTOS =
        List.of(new FotoElegida("places/x/photos/una", "1200x900|Alguien"));

    /** Lo que separa este barrido del de fotos: no se baja ni una. */
    @Test
    void noBajaNingunaFoto() {
        when(repository.findByPhotoUrlIsNull()).thenReturn(List.of(sinFoto(1, "Con fotos")));
        when(placesClient.fotosDe(anyString())).thenReturn(TIENE_FOTOS);

        service.revisarFichas();

        verify(placesClient, never()).downloadPhoto(anyString());
        verify(quotaGuard, never()).record(PlacesCallType.PHOTO);
    }

    /**
     * Ni le presta la de otra sucursal de la cadena, por más que eso no cueste llamadas.
     *
     * Ponerle portada lo saca de la lista de los que no tienen, que es justo lo que se
     * está tratando de contar.
     */
    @Test
    void tampocoLePrestaLaFotoDeOtraSucursal() {
        BurgerJoint sucursal = sinFoto(1, "Mostaza Quilmes");
        when(repository.findByPhotoUrlIsNull()).thenReturn(List.of(sucursal));
        when(repository.findByPhotoUrlIsNotNull()).thenReturn(List.of(
            BurgerJoint.builder().id(9L).placeId("ChIJ9").name("Mostaza Lanús")
                .address("Otra").area("Lanús").photoUrl("/api/place-photos/mostaza.jpg").build()));
        when(placesClient.fotosDe(anyString())).thenReturn(List.of());

        service.revisarFichas();

        assertThat(guardado().getPhotoUrl()).isNull();
    }

    /** El número que se busca: de cuántos Google no tiene nada. */
    @Test
    void cuentaDeCuantosGoogleNoTieneNingunaFoto() {
        when(repository.findByPhotoUrlIsNull()).thenReturn(List.of(
            sinFoto(1, "Tiene"), sinFoto(2, "No tiene"), sinFoto(3, "Tiene también")));
        when(placesClient.fotosDe("ChIJ1")).thenReturn(TIENE_FOTOS);
        when(placesClient.fotosDe("ChIJ2")).thenReturn(List.of());
        when(placesClient.fotosDe("ChIJ3")).thenReturn(TIENE_FOTOS);

        CensoDeFichas censo = service.revisarFichas();

        assertThat(censo.preguntados()).isEqualTo(3);
        assertThat(censo.sinNingunaFoto()).isEqualTo(1);
        assertThat(censo.conFotosParaBajar()).isEqualTo(2);
        assertThat(censo.sinPreguntar()).isZero();
    }

    /** Y queda anotado, que es lo que la limpieza mira para borrarlo. */
    @Test
    void anotaAlQueNoTieneNinguna() {
        when(repository.findByPhotoUrlIsNull()).thenReturn(List.of(sinFoto(1, "Sin nada")));
        when(placesClient.fotosDe(anyString())).thenReturn(List.of());

        service.revisarFichas();

        assertThat(guardado().isSinFotosEnGoogle()).isTrue();
    }

    /**
     * Y al que sí tiene se le borra la anotación vieja.
     *
     * Un local que recién abrió puede no tener ninguna hoy y tener diez el mes que viene.
     * Sin esto, la anotación de la primera vez lo condenaría a que la limpieza lo borre.
     */
    @Test
    void leBorraLaAnotacionAlQueYaTieneFotos() {
        BurgerJoint local = sinFoto(1, "Ya tiene");
        local.setSinFotosEnGoogle(true);
        when(repository.findByPhotoUrlIsNull()).thenReturn(List.of(local));
        when(placesClient.fotosDe(anyString())).thenReturn(TIENE_FOTOS);

        service.revisarFichas();

        assertThat(guardado().isSinFotosEnGoogle()).isFalse();
    }

    /** Sin cambios no se guarda: son mil doscientas escrituras que no hacen nada. */
    @Test
    void noGuardaAlQueYaEstabaBienAnotado() {
        BurgerJoint alDia = sinFoto(1, "Tiene");
        alDia.setFotosEnGoogle(TIENE_FOTOS.size());
        when(repository.findByPhotoUrlIsNull()).thenReturn(List.of(alDia));
        when(placesClient.fotosDe(anyString())).thenReturn(TIENE_FOTOS);

        service.revisarFichas();

        verify(repository, never()).save(any(BurgerJoint.class));
    }

    /**
     * Pero si cambió cuántas fotos tiene, sí: es el dato que decide si el local vale la
     * pena, y uno que pasó de tres a diez dejó de ser el mismo caso.
     */
    @Test
    void guardaCuandoCambioLaCantidadDeFotos() {
        BurgerJoint conOtraCuenta = sinFoto(1, "Tiene más");
        conOtraCuenta.setFotosEnGoogle(99);
        when(repository.findByPhotoUrlIsNull()).thenReturn(List.of(conOtraCuenta));
        when(placesClient.fotosDe(anyString())).thenReturn(TIENE_FOTOS);

        service.revisarFichas();

        assertThat(guardado().getFotosEnGoogle()).isEqualTo(TIENE_FOTOS.size());
    }

    /** Y el que nunca se preguntó queda con el número, que antes no existía. */
    @Test
    void anotaCuantasFotosTiene() {
        when(repository.findByPhotoUrlIsNull()).thenReturn(List.of(sinFoto(1, "Nuevo")));
        when(placesClient.fotosDe(anyString())).thenReturn(TIENE_FOTOS);

        service.revisarFichas();

        assertThat(guardado().getFotosEnGoogle()).isEqualTo(TIENE_FOTOS.size());
    }

    /** Si la cuota de fichas se termina, se dice cuántos quedaron sin preguntar. */
    @Test
    void avisaCuantosQuedaronSinPreguntar() {
        when(repository.findByPhotoUrlIsNull()).thenReturn(List.of(
            sinFoto(1, "Uno"), sinFoto(2, "Dos"), sinFoto(3, "Tres")));
        when(placesClient.fotosDe(anyString())).thenReturn(TIENE_FOTOS);
        when(quotaGuard.canCall(PlacesCallType.DETAILS)).thenReturn(true, true, false);

        CensoDeFichas censo = service.revisarFichas();

        assertThat(censo.preguntados()).isEqualTo(2);
        assertThat(censo.sinPreguntar()).isEqualTo(1);
    }

    /** Sin clave no sale a preguntar nada, igual que los otros disparadores. */
    @Test
    void sinClaveNoHaceNada() {
        PlacesProperties sinClave = new PlacesProperties();
        PlacesSyncService servicio = new PlacesSyncService(
            sinClave, placesClient, quotaGuard, photoStorage, repository, new Barrios(),
            new Zonas(new Barrios(), sinClave), mock(RatingRepository.class),
            mock(WishlistRepository.class), mock(SavedTourRepository.class),
            new FastFoodMarker(repository, sinClave));

        assertThat(servicio.revisarFichas().warning()).contains("GOOGLE_MAPS_API_KEY");
        verify(placesClient, never()).fotosDe(anyString());
    }

    private BurgerJoint guardado() {
        ArgumentCaptor<BurgerJoint> capturado = ArgumentCaptor.forClass(BurgerJoint.class);
        verify(repository).save(capturado.capture());
        return capturado.getValue();
    }
}
