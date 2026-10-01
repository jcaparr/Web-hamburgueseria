package com.hamburguesas.places;

import com.hamburguesas.repository.BurgerJointRepository;
import com.hamburguesas.repository.RatingRepository;
import com.hamburguesas.repository.SavedTourRepository;
import com.hamburguesas.repository.WishlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Qué hace el barrido cuando Google contesta mal.
 *
 * Esto se escribió después de perder dos corridas. Cualquier error cortaba el barrido
 * entero, y como las zonas se recorren siempre en el mismo orden, siempre moría por la
 * mitad: las últimas de la lista —todo el corredor norte, La Plata, Berisso— no entraron
 * nunca, y cada intento se llevaba igual las búsquedas que ya había gastado.
 *
 * Un 503 es Google que no está por un rato. Un 429 es que nos pasamos del ritmo. No se
 * tratan igual.
 */
class BarridoAnteUnErrorTest {

    private PlacesClient placesClient;
    private BurgerJointRepository repository;
    private PlacesSyncService service;

    private static final List<String> TRES_ZONAS = List.of("Pilar", "Garín", "La Plata");

    @BeforeEach
    void setUp() {
        PlacesProperties properties = new PlacesProperties();
        properties.setApiKey("clave-de-prueba");
        properties.getSync().setAreas(TRES_ZONAS);
        properties.getSync().setMaxPagesPerArea(1);
        properties.getSync().setQueryTemplates(List.of("hamburguesería en {barrio}"));
        properties.getSync().setDelayBetweenCallsMs(0);

        placesClient = mock(PlacesClient.class);
        repository = mock(BurgerJointRepository.class);
        PlacesQuotaGuard quotaGuard = mock(PlacesQuotaGuard.class);

        when(quotaGuard.canCall(any())).thenReturn(true);
        when(repository.findByPhotoUrlIsNull()).thenReturn(List.of());
        when(repository.findByPhotoUrlIsNotNull()).thenReturn(List.of());
        when(repository.conFotoElegidaConUnaReglaVieja(anyInt())).thenReturn(List.of());
        when(repository.findByPlaceId(anyString())).thenReturn(Optional.empty());
        when(repository.nombresDeRubro(anyString())).thenReturn(List.of());

        service = ServicioArmado.armar(
            properties, placesClient, quotaGuard, mock(PhotoStorage.class), repository,
            new Zonas(new Barrios(), properties), mock(RatingRepository.class),
            mock(WishlistRepository.class), mock(SavedTourRepository.class),
            new FastFoodMarker(repository, properties));
    }

    private static PlacesSearchResult unLocalDePalermo() {
        return new PlacesSearchResult(List.of(new PlacesSearchResult.Place(
            "ChIJ-uno", "Un local", "Costa Rica 5827", -34.5900, -58.4270, null, null,
            "hamburger_restaurant", Set.of("hamburger_restaurant"))), null);
    }

    /**
     * El caso que costó dos corridas: un 503 en la primera zona dejaba sin recorrer las
     * otras dos. Ahora se saltea esa consulta y sigue.
     */
    @Test
    void un503SalteaLaConsultaYSigueConLasDemas() {
        when(placesClient.searchText(anyString(), any()))
            .thenThrow(HttpServerErrorException.create(
                HttpStatus.SERVICE_UNAVAILABLE, "no disponible", null, null, null))
            .thenReturn(unLocalDePalermo())
            .thenReturn(unLocalDePalermo());

        PlacesSyncReport report = service.sync(false);

        verify(placesClient, times(TRES_ZONAS.size())).searchText(anyString(), any());
        assertThat(report.created()).isEqualTo(1);
    }

    /** Y queda dicho, para que una zona con menos locales de los que debería se explique. */
    @Test
    void avisaCuantasConsultasQuedaronSinRespuesta() {
        when(placesClient.searchText(anyString(), any()))
            .thenThrow(HttpServerErrorException.create(
                HttpStatus.SERVICE_UNAVAILABLE, "no disponible", null, null, null))
            .thenReturn(unLocalDePalermo())
            .thenReturn(unLocalDePalermo());

        assertThat(service.sync(false).warning()).contains("1 consultas");
    }

    /**
     * Un 429 sí frena todo: nos pasamos del ritmo que Google acepta, y las que siguen
     * van a fallar igual. Insistir es gastar llamadas para recibir el mismo error.
     */
    @Test
    void un429FrenaElBarrido() {
        when(placesClient.searchText(anyString(), any()))
            .thenThrow(HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS, "muchas", null, null, null));

        PlacesSyncReport report = service.sync(false);

        verify(placesClient, times(1)).searchText(anyString(), any());
        assertThat(report.warning()).contains("429");
    }

    /** Una clave que no sirve tampoco se arregla reintentando. */
    @Test
    void unaClaveRechazadaFrenaElBarrido() {
        when(placesClient.searchText(anyString(), any()))
            .thenThrow(HttpClientErrorException.create(
                HttpStatus.FORBIDDEN, "prohibido", null, null, null));

        assertThat(service.sync(false).warning()).contains("403");
        verify(placesClient, times(1)).searchText(anyString(), any());
    }

    /**
     * Pero si falla una y otra vez, Google está caído y se deja de insistir.
     *
     * Saltear es para un hipo suelto. Recorrer las 89 zonas para juntar 89 errores no le
     * sirve a nadie, y deja el informe diciendo que terminó.
     */
    @Test
    void siFallaDiezVecesSeguidasSeRinde() {
        PlacesProperties properties = new PlacesProperties();
        properties.setApiKey("clave-de-prueba");
        properties.getSync().setAreas(List.of(
            "z1", "z2", "z3", "z4", "z5", "z6", "z7", "z8", "z9", "z10", "z11", "z12"));
        properties.getSync().setMaxPagesPerArea(1);
        properties.getSync().setQueryTemplates(List.of("hamburguesería en {barrio}"));
        properties.getSync().setDelayBetweenCallsMs(0);

        PlacesQuotaGuard quotaGuard = mock(PlacesQuotaGuard.class);
        when(quotaGuard.canCall(any())).thenReturn(true);

        PlacesSyncService servicio = ServicioArmado.armar(
            properties, placesClient, quotaGuard, mock(PhotoStorage.class), repository,
            new Zonas(new Barrios(), properties), mock(RatingRepository.class),
            mock(WishlistRepository.class), mock(SavedTourRepository.class),
            new FastFoodMarker(repository, properties));

        when(placesClient.searchText(anyString(), any()))
            .thenThrow(HttpServerErrorException.create(
                HttpStatus.SERVICE_UNAVAILABLE, "no disponible", null, null, null));

        PlacesSyncReport report = servicio.sync(false);

        verify(placesClient, times(10)).searchText(anyString(), any());
        assertThat(report.warning()).contains("10 veces seguidas");
    }

    /** Y la cuenta de fallas se reinicia: diez sueltas a lo largo del día no son una caída. */
    @Test
    void unaRespuestaBuenaReiniciaLaCuenta() {
        when(placesClient.searchText(anyString(), any()))
            .thenThrow(HttpServerErrorException.create(
                HttpStatus.SERVICE_UNAVAILABLE, "no disponible", null, null, null))
            .thenReturn(unLocalDePalermo())
            .thenThrow(HttpServerErrorException.create(
                HttpStatus.SERVICE_UNAVAILABLE, "no disponible", null, null, null));

        PlacesSyncReport report = service.sync(false);

        verify(placesClient, times(TRES_ZONAS.size())).searchText(anyString(), any());
        assertThat(report.warning()).contains("2 consultas");
    }

    /** Sin errores no hay aviso: un barrido limpio no tiene nada que contar. */
    @Test
    void sinErroresNoAvisaNada() {
        when(placesClient.searchText(anyString(), any())).thenReturn(unLocalDePalermo());

        assertThat(service.sync(false).warning()).isNull();
    }
}
