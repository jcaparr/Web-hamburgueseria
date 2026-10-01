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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * La limpieza corriendo sola, sin el barrido.
 *
 * Es la primera mitad del barrido suelta. Existe porque esa mitad no cuesta ninguna
 * búsqueda y la otra cuesta mil novecientas: cuando lo que hace falta es aplicar lo que
 * un censo de fichas ya averiguó, correr el barrido entero es pagar la parte cara para
 * ejecutar la gratis.
 */
class LimpiezaSinBarridoTest {

    private PlacesClient placesClient;
    private BurgerJointRepository repository;
    private PlacesQuotaGuard quotaGuard;
    private RatingRepository ratingRepository;
    private WishlistRepository wishlistRepository;
    private SavedTourRepository savedTourRepository;
    private PlacesSyncService service;

    @BeforeEach
    void setUp() {
        PlacesProperties properties = new PlacesProperties();
        properties.setApiKey("clave-de-prueba");
        properties.getSync().setDelayBetweenCallsMs(0);

        placesClient = mock(PlacesClient.class);
        repository = mock(BurgerJointRepository.class);
        quotaGuard = mock(PlacesQuotaGuard.class);
        ratingRepository = mock(RatingRepository.class);
        wishlistRepository = mock(WishlistRepository.class);
        savedTourRepository = mock(SavedTourRepository.class);

        when(quotaGuard.canCall(any())).thenReturn(true);
        when(repository.findAll()).thenReturn(List.of());
        when(repository.findByFastFoodFalse()).thenReturn(List.of());
        when(repository.nombresDeRubro(anyString())).thenReturn(List.of());
        when(repository.findByPlaceId(anyString())).thenReturn(Optional.empty());
        when(ratingRepository.existsByBurgerJoint_Id(anyLong())).thenReturn(false);
        when(wishlistRepository.existsByBurgerJoint_Id(anyLong())).thenReturn(false);
        when(savedTourRepository.estaEnAlgunTour(anyLong())).thenReturn(false);

        service = ServicioArmado.armar(
            properties, placesClient, quotaGuard, mock(PhotoStorage.class), repository,
            new Zonas(new Barrios(), properties), ratingRepository,
            wishlistRepository, savedTourRepository,
            new FastFoodMarker(repository, properties));
    }

    /** Un local de Palermo del que Google dice no tener ninguna foto. */
    private static BurgerJoint sinFotosEnGoogle() {
        BurgerJoint joint = BurgerJoint.builder()
            .id(1L).placeId("ChIJ1").name("Sin nada")
            .address("Costa Rica 5827, CABA").area("Palermo")
            .latitude(-34.5900).longitude(-58.4270)
            .googlePrimaryType("hamburger_restaurant")
            .build();
        joint.setSinFotosEnGoogle(true);
        return joint;
    }

    /** Lo que se vino a hacer: aplicar lo que el censo marcó. */
    @Test
    void borraLosQueGoogleNoTieneFotografiados() {
        when(repository.findAll()).thenReturn(List.of(sinFotosEnGoogle()));

        LimpiezaResult resultado = service.limpiar();

        verify(repository).delete(any(BurgerJoint.class));
        assertThat(resultado.borrados()).isEqualTo(1);
    }

    /** Y lo que la hace valer la pena: no gasta una sola búsqueda. */
    @Test
    void noGastaNingunaBusqueda() {
        when(repository.findAll()).thenReturn(List.of(sinFotosEnGoogle()));

        service.limpiar();

        verify(placesClient, never()).searchText(anyString(), any());
        verify(quotaGuard, never()).record(PlacesCallType.SEARCH);
    }

    /** Ni una foto, ni una ficha. */
    @Test
    void noGastaNingunaFotoNiFicha() {
        when(repository.findAll()).thenReturn(List.of(sinFotosEnGoogle()));

        service.limpiar();

        verify(placesClient, never()).downloadPhoto(anyString());
        verify(placesClient, never()).fotosDe(anyString());
        verify(quotaGuard, never()).record(PlacesCallType.PHOTO);
        verify(quotaGuard, never()).record(PlacesCallType.DETAILS);
    }

    /**
     * Un local que alguien puntuó o guardó se queda, aunque no corresponda.
     *
     * Es la misma protección que ya tenía el barrido, y acá importa igual: borrar sigue
     * siendo definitivo y un recorrido guardado al que le falta una parada no se puede
     * rehacer.
     */
    @Test
    void noBorraElQueAlguienTieneGuardado() {
        when(repository.findAll()).thenReturn(List.of(sinFotosEnGoogle()));
        when(wishlistRepository.existsByBurgerJoint_Id(anyLong())).thenReturn(true);

        LimpiezaResult resultado = service.limpiar();

        verify(repository, never()).delete(any(BurgerJoint.class));
        assertThat(resultado.borrados()).isZero();
    }

    /** Y el que sí tiene fotos no se toca. */
    @Test
    void noBorraAlQueTieneFotos() {
        BurgerJoint conFotos = sinFotosEnGoogle();
        conFotos.setSinFotosEnGoogle(false);
        when(repository.findAll()).thenReturn(List.of(conFotos));

        LimpiezaResult resultado = service.limpiar();

        verify(repository, never()).delete(any(BurgerJoint.class));
        assertThat(resultado.borrados()).isZero();
    }

    /** Sin nada guardado no hace nada y no falla. */
    @Test
    void conLaBaseVaciaNoHaceNada() {
        LimpiezaResult resultado = service.limpiar();

        assertThat(resultado.borrados()).isZero();
        assertThat(resultado.corregidos()).isZero();
    }
}
