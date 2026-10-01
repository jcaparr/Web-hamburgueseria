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
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * La sucursal de una cadena usa la foto de una hermana, sin pedirle nada a Google.
 *
 * Antes era al revés: se le pedía la propia y se prestaba solo si Google no tenía
 * ninguna, porque la foto propia de la sucursal es mejor que la prestada. Sigue siendo
 * cierto y ya no alcanza: hay 1.688 locales esperando portada y 911 fotos hasta fin de
 * mes, así que cada foto gastada en un McDonald's es una hamburguesería de barrio que se
 * queda con el recuadro de iniciales. Y entre dos sucursales de la misma cadena, la foto
 * es prácticamente la misma.
 */
class FotoPrestadaEntreSucursalesTest {

    private PlacesClient placesClient;
    private BurgerJointRepository repository;
    private PlacesSyncService service;

    private static final String FOTO_DE_LA_HERMANA = "/api/place-photos/mostaza-lanus.jpg";

    @BeforeEach
    void setUp() {
        PlacesProperties properties = new PlacesProperties();
        properties.setApiKey("clave-de-prueba");
        properties.getSync().setDelayBetweenCallsMs(0);
        // La marca es lo que junta a las sucursales, asi que sin la lista no hay prestamo.
        // Antes estas pruebas pasaban con la lista vacia porque alcanzaba con la columna
        // de comida rapida, que en produccion sale justamente de esta lista.
        properties.setFastFoodBrands(List.of("mostaza", "mcdonalds"));
        properties.setMarcasConSucursales(List.of("bigpons"));

        placesClient = mock(PlacesClient.class);
        repository = mock(BurgerJointRepository.class);
        PlacesQuotaGuard quotaGuard = mock(PlacesQuotaGuard.class);
        PhotoStorage photoStorage = mock(PhotoStorage.class);

        when(quotaGuard.canCall(any())).thenReturn(true);
        when(repository.findByPhotoUrlIsNull()).thenReturn(List.of());
        when(repository.conFotoElegidaConUnaReglaVieja(anyInt())).thenReturn(List.of());
        when(repository.findByPlaceId(anyString())).thenReturn(Optional.empty());
        when(repository.nombresDeRubro(anyString())).thenReturn(List.of());
        when(photoStorage.save(anyString(), any())).thenReturn("/api/place-photos/propia.jpg");

        // La hermana que ya tiene foto, que es de donde sale la prestada.
        when(repository.findByPhotoUrlIsNotNull()).thenReturn(List.of(
            BurgerJoint.builder().id(9L).placeId("ChIJ9").name("Mostaza - Lanús")
                .address("Otra dirección").area("Lanús").fastFood(true)
                .photoUrl(FOTO_DE_LA_HERMANA).build()));

        service = new PlacesSyncService(
            properties, placesClient, quotaGuard, photoStorage, repository, new Barrios(),
            new Zonas(new Barrios(), properties), mock(RatingRepository.class),
            mock(WishlistRepository.class), mock(SavedTourRepository.class),
            new FastFoodMarker(repository, properties));
    }

    private static BurgerJoint sinFoto(String nombre, boolean deCadena) {
        return BurgerJoint.builder()
            .id(1L).placeId("ChIJ1").name(nombre)
            .address("Una dirección").area("Quilmes").fastFood(deCadena)
            .build();
    }

    private BurgerJoint guardado() {
        ArgumentCaptor<BurgerJoint> capturado = ArgumentCaptor.forClass(BurgerJoint.class);
        verify(repository, atLeastOnce()).save(capturado.capture());
        return capturado.getValue();
    }

    /** Lo que ahorra: ni la ficha ni la foto. Las dos cuotas quedan intactas. */
    @Test
    void laSucursalDeCadenaNoLeCuestaNingunaLlamada() {
        when(repository.findByPhotoUrlIsNull())
            .thenReturn(List.of(sinFoto("Mostaza - Quilmes", true)));

        service.revisarFotos();

        verify(placesClient, never()).fotosDe(anyString());
        verify(placesClient, never()).downloadPhoto(anyString());
    }

    /** Y queda con portada igual, que es el punto. */
    @Test
    void laSucursalDeCadenaQuedaConLaFotoDeLaHermana() {
        when(repository.findByPhotoUrlIsNull())
            .thenReturn(List.of(sinFoto("Mostaza - Quilmes", true)));

        service.revisarFotos();

        assertThat(guardado().getPhotoUrl()).isEqualTo(FOTO_DE_LA_HERMANA);
    }

    /**
     * Un local que no es de cadena sigue recibiendo su propia foto.
     *
     * Es el caso que importa: las 911 fotos que quedan son para estos. Si el ahorro de
     * las cadenas se les aplicara también, la mitad de las hamburgueserías de barrio
     * terminaría con la portada de otro local que casualmente se llama parecido.
     */
    @Test
    void unLocalIndependienteSigueRecibiendoSuPropiaFoto() {
        when(repository.findByPhotoUrlIsNull())
            .thenReturn(List.of(sinFoto("La Birra Bar", false)));
        when(placesClient.fotosDe(anyString())).thenReturn(List.of(
            new FotoElegida("places/x/photos/propia", "1200x900|Alguien")));
        when(placesClient.downloadPhoto(anyString())).thenReturn(new byte[] {1, 2, 3});

        service.revisarFotos();

        verify(placesClient).fotosDe("ChIJ1");
        assertThat(guardado().getPhotoUrl()).isEqualTo("/api/place-photos/propia.jpg");
    }

    /** Una cadena sin ninguna hermana con foto tampoco se queda sin nada: se le pide. */
    @Test
    void siLaCadenaNoTieneHermanaConFotoSeLePideAGoogle() {
        when(repository.findByPhotoUrlIsNotNull()).thenReturn(List.of());
        when(repository.findByPhotoUrlIsNull())
            .thenReturn(List.of(sinFoto("Mostaza - Quilmes", true)));
        when(placesClient.fotosDe(anyString())).thenReturn(List.of(
            new FotoElegida("places/x/photos/propia", "1200x900|Alguien")));
        when(placesClient.downloadPhoto(anyString())).thenReturn(new byte[] {1, 2, 3});

        service.revisarFotos();

        verify(placesClient).fotosDe("ChIJ1");
        assertThat(guardado().getPhotoUrl()).isEqualTo("/api/place-photos/propia.jpg");
    }

    /** Y la cuota de fotos no se toca por una sucursal prestada. */
    @Test
    void prestarNoGastaCuotaDeFotos() {
        PlacesQuotaGuard quotaGuard = mock(PlacesQuotaGuard.class);
        when(quotaGuard.canCall(any())).thenReturn(true);
        PlacesProperties properties = new PlacesProperties();
        properties.setApiKey("clave-de-prueba");
        properties.getSync().setDelayBetweenCallsMs(0);
        properties.setFastFoodBrands(List.of("mostaza", "mcdonalds"));

        when(repository.findByPhotoUrlIsNull())
            .thenReturn(List.of(sinFoto("Mostaza - Quilmes", true)));

        PlacesSyncService servicio = new PlacesSyncService(
            properties, placesClient, quotaGuard, mock(PhotoStorage.class), repository,
            new Barrios(), new Zonas(new Barrios(), properties), mock(RatingRepository.class),
            mock(WishlistRepository.class), mock(SavedTourRepository.class),
            new FastFoodMarker(repository, properties));

        servicio.revisarFotos();

        verify(quotaGuard, never()).record(PlacesCallType.PHOTO);
        verify(quotaGuard, never()).record(PlacesCallType.DETAILS);
    }

    // ---- la marca, y no el nombre entero, es lo que junta a las sucursales ----

    /**
     * La sucursal que lleva el barrio en el nombre también recibe la foto.
     *
     * Es lo que no pasaba: "McDonald's" y "McDonald's Abasto Patio de Comidas" son la
     * misma cadena para el filtro de Explorar y eran dos distintas para la foto, porque
     * la clave era el nombre entero. Quedaban dieciocho sucursales con el recuadro de
     * iniciales teniendo setenta y nueve hermanas con portada.
     */
    @Test
    void laSucursalConElBarrioEnElNombreTambienRecibeLaFoto() {
        when(repository.findByPhotoUrlIsNotNull()).thenReturn(List.of(
            BurgerJoint.builder().id(9L).placeId("ChIJ9").name("McDonald's")
                .address("Otra dirección").area("Lanús").fastFood(true)
                .photoUrl(FOTO_DE_LA_HERMANA).build()));
        when(repository.findByPhotoUrlIsNull()).thenReturn(List.of(
            sinFoto("McDonald's Abasto Patio de Comidas", true)));

        service.prestarFotos();

        assertThat(guardado().getPhotoUrl()).isEqualTo(FOTO_DE_LA_HERMANA);
    }

    /**
     * Una marca con sucursales que no es comida rápida también presta.
     *
     * Es el motivo de que sean dos listas: para que Big Pons pueda compartir la portada
     * sin que apagar las cadenas en Explorar la esconda.
     */
    @Test
    void unaMarcaConSucursalesQueNoEsComidaRapidaTambienPresta() {
        when(repository.findByPhotoUrlIsNotNull()).thenReturn(List.of(
            BurgerJoint.builder().id(9L).placeId("ChIJ9").name("Big Pons")
                .address("Otra dirección").area("Palermo").fastFood(false)
                .photoUrl(FOTO_DE_LA_HERMANA).build()));
        when(repository.findByPhotoUrlIsNull()).thenReturn(List.of(
            sinFoto("Big Pons", false)));

        service.prestarFotos();

        BurgerJoint quedo = guardado();
        assertThat(quedo.getPhotoUrl()).isEqualTo(FOTO_DE_LA_HERMANA);
        // Y sigue sin ser comida rápida, que es lo que la deja visible en Explorar.
        assertThat(quedo.isFastFood()).isFalse();
    }

    /** Dos marcas distintas no se prestan nada, aunque las dos estén en las listas. */
    @Test
    void dosMarcasDistintasNoSePrestanNada() {
        when(repository.findByPhotoUrlIsNull()).thenReturn(List.of(
            sinFoto("Big Pons", false)));

        assertThat(service.prestarFotos().prestadas()).isZero();
        verify(repository, never()).save(any());
    }

    /** Un local de ninguna marca conocida no recibe nada: la foto sería de un desconocido. */
    @Test
    void unLocalSinMarcaConocidaNoRecibeNada() {
        when(repository.findByPhotoUrlIsNull()).thenReturn(List.of(
            sinFoto("Hamburguesería", false)));

        assertThat(service.prestarFotos().prestadas()).isZero();
        verify(repository, never()).save(any());
    }

    // ---- el préstamo suelto ----

    /** No le pide nada a Google: la foto ya está bajada. */
    @Test
    void prestarSueltoNoLlamaAGoogle() {
        when(repository.findByPhotoUrlIsNull())
            .thenReturn(List.of(sinFoto("Mostaza - Quilmes", true)));

        service.prestarFotos();

        verify(placesClient, never()).fotosDe(anyString());
        verify(placesClient, never()).downloadPhoto(anyString());
    }

    /**
     * Y no pisa ninguna portada: solo mira los locales que no tienen.
     *
     * Es la diferencia con la revisión de fotos, que vuelve a elegir las viejas. Una
     * sucursal con foto propia se la queda, que para eso se la bajamos.
     */
    @Test
    void prestarNoPisaLaPortadaDeNadie() {
        BurgerJoint laQueTiene = BurgerJoint.builder().id(9L).placeId("ChIJ9")
            .name("Mostaza - Lanús").address("Otra dirección").area("Lanús")
            .fastFood(true).photoUrl(FOTO_DE_LA_HERMANA).build();
        when(repository.findByPhotoUrlIsNotNull()).thenReturn(List.of(laQueTiene));
        when(repository.findByPhotoUrlIsNull())
            .thenReturn(List.of(sinFoto("Mostaza - Quilmes", true)));

        service.prestarFotos();

        // La única que se guarda es la que no tenía.
        ArgumentCaptor<BurgerJoint> capturado = ArgumentCaptor.forClass(BurgerJoint.class);
        verify(repository, atLeastOnce()).save(capturado.capture());
        assertThat(capturado.getAllValues()).allMatch(j -> j.getId() == 1L);
        assertThat(laQueTiene.getPhotoUrl()).isEqualTo(FOTO_DE_LA_HERMANA);
    }

    /** Cuenta las que presta y las que siguen sin portada, para poder medirlo. */
    @Test
    void cuentaLasPrestadasYLasQueSiguenSinPortada() {
        when(repository.findByPhotoUrlIsNull()).thenReturn(List.of(
            sinFoto("Mostaza - Quilmes", true),
            BurgerJoint.builder().id(2L).placeId("ChIJ2").name("Una de barrio")
                .address("Una dirección").area("Quilmes").fastFood(false).build()));

        PlacesSyncService.FotosPrestadas resultado = service.prestarFotos();

        assertThat(resultado.prestadas()).isEqualTo(1);
        assertThat(resultado.siguenSinPortada()).isEqualTo(1);
    }
}
