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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Barrer una zona sola, la que se pida (#222).
 *
 * Es como se suman locales ahora que la página está poblada: a mano, o barriendo un
 * barrio, una ciudad, una provincia o un radio adonde todavía no se llegó. El barrido
 * general era para llenarla y no se vuelve a usar.
 */
class BarridoDeUnaZonaTest {

    private static final String MAR_DEL_PLATA =
        "Av. Constitución 4205, B7600 Mar del Plata, Provincia de Buenos Aires, Argentina";

    private PlacesProperties properties;
    private PlacesClient placesClient;
    private BurgerJointRepository repository;
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
        repository = mock(BurgerJointRepository.class);
        PlacesQuotaGuard quotaGuard = mock(PlacesQuotaGuard.class);

        when(quotaGuard.canCall(any())).thenReturn(true);
        when(repository.findByPhotoUrlIsNull()).thenReturn(List.of());
        when(repository.conFotoElegidaConUnaReglaVieja(anyInt())).thenReturn(List.of());
        when(repository.findByPlaceId(anyString())).thenReturn(Optional.empty());
        when(repository.nombresDeRubro(anyString())).thenReturn(List.of());

        service = ServicioArmado.armar(
            properties, placesClient, quotaGuard, mock(PhotoStorage.class), repository,
            new Zonas(new Barrios(), properties), mock(RatingRepository.class),
            mock(WishlistRepository.class), mock(SavedTourRepository.class),
            new FastFoodMarker(repository, properties));
    }

    private static PlacesSearchResult.Place hamburgueseria(String nombre, String direccion,
                                                          double lat, double lon) {
        return new PlacesSearchResult.Place("ChIJ-" + nombre, nombre, direccion, lat, lon,
            null, null, "hamburger_restaurant", Set.of("hamburger_restaurant"));
    }

    private void googleDevuelve(PlacesSearchResult.Place... lugares) {
        PlacesSearchResult resultado = new PlacesSearchResult(List.of(lugares), null);
        when(placesClient.searchText(anyString(), any())).thenReturn(resultado);
        when(placesClient.searchText(anyString(), any(), eq(true), any(Circulo.class))).thenReturn(resultado);
    }

    private List<BurgerJoint> guardados() {
        ArgumentCaptor<BurgerJoint> capturados = ArgumentCaptor.forClass(BurgerJoint.class);
        verify(repository, org.mockito.Mockito.atLeast(0)).save(capturados.capture());
        return capturados.getAllValues();
    }

    /**
     * La zona va tal cual se la escribe, sin ", Buenos Aires" detrás: tiene que poder ser
     * de cualquier provincia. Y son las mismas formas de preguntar del barrido general.
     */
    @Test
    void preguntaDeCadaFormaPorLaZonaTalCual() {
        properties.getSync().setQueryTemplates(List.of(
            "hamburguesería en {barrio}", "smash burger en {barrio}"));
        googleDevuelve();

        service.barrerZona("Córdoba, Argentina", null, false);

        ArgumentCaptor<String> consultas = ArgumentCaptor.forClass(String.class);
        verify(placesClient, times(2)).searchText(consultas.capture(), any());
        assertThat(consultas.getAllValues()).containsExactly(
            "hamburguesería en Córdoba, Argentina", "smash burger en Córdoba, Argentina");
    }

    /** Lo que trae entra aunque quede a más de 75 km del Obelisco. */
    @Test
    void guardaLoQueQuedaLejosDelObelisco() {
        googleDevuelve(hamburgueseria("Burger del Puerto", MAR_DEL_PLATA, -37.9694, -57.5455));

        PlacesSyncReport reporte = service.barrerZona("Mar del Plata, Buenos Aires", null, false);

        assertThat(reporte.created()).isEqualTo(1);
        assertThat(guardados()).extracting(BurgerJoint::getArea).containsExactly("Mar del Plata");
    }

    /**
     * No hace la limpieza general. Para sumar una zona no hace falta revisar toda la
     * base, y la limpieza puede borrar.
     */
    @Test
    void noHaceLaLimpiezaDeTodaLaBase() {
        googleDevuelve();
        BurgerJoint sinZona = BurgerJoint.builder().id(1L).placeId("ChIJ-viejo").name("Viejo")
            .address("Una dirección").latitude(7.06).longitude(-73.09)
            .googlePrimaryType("hamburger_restaurant").build();
        when(repository.findAll()).thenReturn(List.of(sinZona));

        service.barrerZona("Mar del Plata, Buenos Aires", null, false);

        verify(repository, never()).delete(any(BurgerJoint.class));
    }

    /**
     * Con un radio, se le pide a Google que priorice ese círculo y entra solo lo de
     * adentro: priorizar no es limitar, y Google igual devuelve lo que le parece.
     */
    @Test
    void conUnRadioEntraSoloLoDeAdentro() {
        googleDevuelve(
            hamburgueseria("Burger del Puerto", MAR_DEL_PLATA, -37.9694, -57.5455),
            hamburgueseria("The Burger Company", "Honduras 4733, CABA, Argentina", -34.5900, -58.4270));
        Circulo alrededorDeMarDelPlata = new Circulo(-38.0055, -57.5426, 15);

        PlacesSyncReport reporte = service.barrerZona("hamburguesas", alrededorDeMarDelPlata, false);

        verify(placesClient).searchText(anyString(), any(), eq(true), eq(alrededorDeMarDelPlata));
        assertThat(reporte.created()).isEqualTo(1);
        assertThat(guardados()).extracting(BurgerJoint::getName).containsExactly("Burger del Puerto");
    }

    /** Lo que no es de Argentina sigue sin entrar: "Córdoba" sola puede traer España. */
    @Test
    void noGuardaLoQueNoEsDeArgentina() {
        googleDevuelve(hamburgueseria("Burger Córdoba", "Calle Gondomar 1, 14003 Córdoba, España",
            37.8847, -4.7792));

        PlacesSyncReport reporte = service.barrerZona("Córdoba", null, false);

        assertThat(reporte.created()).isZero();
        verify(repository, never()).save(any(BurgerJoint.class));
    }

    /** Sin zona no sale a buscar: serían siete búsquedas por nada. */
    @Test
    void sinZonaNoBusca() {
        PlacesSyncReport reporte = service.barrerZona("  ", null, false);

        assertThat(reporte.warning()).contains("qué zona");
        verify(placesClient, never()).searchText(anyString(), any());
    }

    /** Un radio de cero o negativo es un error de quien pidió, y no gasta búsquedas. */
    @Test
    void unRadioSinSentidoNoBusca() {
        PlacesSyncReport reporte = service.barrerZona("Pilar", new Circulo(-34.45, -58.91, 0), false);

        assertThat(reporte.warning()).contains("radio");
        verify(placesClient, never()).searchText(anyString(), any(), eq(true), any(Circulo.class));
    }
}
