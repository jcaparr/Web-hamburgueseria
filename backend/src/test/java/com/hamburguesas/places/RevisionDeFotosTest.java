package com.hamburguesas.places;

import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.repository.BurgerJointRepository;
import com.hamburguesas.repository.RatingRepository;
import com.hamburguesas.repository.SavedTourRepository;
import com.hamburguesas.repository.WishlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Random;
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
 * Cubre la revisión de fotos suelta, sin la búsqueda de locales.
 *
 * Existe porque cambiar la regla de elección no cambia ninguna foto por sí solo: lo
 * guardado se queda como está hasta que alguien vuelva a mirarlo. Y hacerlo con la
 * sincronización completa gasta además hasta mil búsquedas recorriendo los 48 barrios,
 * que es la parte cara y la que acá no hace falta.
 *
 * No sale a internet: el cliente de Google está simulado.
 */
class RevisionDeFotosTest {

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

        service = ServicioArmado.armar(
            properties, placesClient, quotaGuard, photoStorage, repository, new Zonas(new Barrios(), properties),
            mock(RatingRepository.class), mock(WishlistRepository.class),
            mock(SavedTourRepository.class), new FastFoodMarker(repository, properties));
    }

    private BurgerJoint local(String nombre, String photoUrl, Integer regla) {
        return BurgerJoint.builder()
            .id(1L).placeId("ChIJ" + nombre).name(nombre)
            .address("Una dirección").area("Palermo")
            .photoUrl(photoUrl).photoName(photoUrl == null ? null : "places/x/photos/vieja")
            .photoFingerprint(photoUrl == null ? null : "huella-de-places/x/photos/vieja")
            .photoRule(regla)
            .build();
    }

    /**
     * Cómo quedó el local después de la revisión.
     *
     * Toma el último guardado y no el único: un local puede guardarse dos veces en la
     * misma pasada —primero para borrarle la anotación de "sin fotos" y después con la
     * foto ya bajada— y lo que se afirma es cómo terminó.
     */
    private BurgerJoint guardado() {
        ArgumentCaptor<BurgerJoint> capturado = ArgumentCaptor.forClass(BurgerJoint.class);
        verify(repository, atLeastOnce()).save(capturado.capture());
        return capturado.getValue();
    }

    /** Lo que separa esto de la sincronización completa: no gasta ni una búsqueda. */
    @Test
    void noSaleABuscarLocalesNuevos() {
        service.revisarFotos();

        verify(placesClient, never()).searchText(anyString(), any());
    }

    /**
     * Un local del que Google no tiene ni una foto queda anotado como tal.
     *
     * Es lo único que distingue "no hay nada" de "no fuimos a buscarlo": de los 102 que
     * no tienen foto guardada, la mitad sí las tiene en Google. Confundirlos escondería
     * medio centenar de hamburgueserías reales por un trabajo nuestro a medias.
     */
    @Test
    void anotaAlLocalDelQueGoogleNoTieneNingunaFoto() {
        when(repository.findByPhotoUrlIsNull()).thenReturn(List.of(local("Sin nada", null, null)));
        when(placesClient.fotosDe(anyString(), any())).thenReturn(java.util.List.of());

        service.revisarFotos();

        assertThat(guardado().isSinFotosEnGoogle()).isTrue();
    }

    /** Y si aparece una foto, se le borra la anotación: un local nuevo hoy no tiene y mañana sí. */
    @Test
    void siAparecenFotosSeLeBorraLaAnotacion() {
        BurgerJoint local = local("Ya tiene", null, null);
        local.setSinFotosEnGoogle(true);
        when(repository.findByPhotoUrlIsNull()).thenReturn(List.of(local));
        when(placesClient.fotosDe(anyString(), any())).thenReturn(java.util.List.of(new FotoElegida("places/x/photos/nueva", "huella-de-places/x/photos/nueva")));
        when(placesClient.downloadPhoto(anyString())).thenReturn(new byte[] {1, 2, 3});
        when(photoStorage.save(anyString(), any())).thenReturn("/api/place-photos/x.jpg");

        service.revisarFotos();

        assertThat(guardado().isSinFotosEnGoogle()).isFalse();
    }

    /**
     * La foto que la regla nueva elige igual que la vieja no se vuelve a bajar.
     *
     * Sería pagarle a Google por el mismo archivo. Alcanza con anotar que ya se revisó,
     * así el trabajo se hace una vez y no en cada corrida.
     */
    @Test
    void siLaReglaNuevaEligeLaMismaFotoNoLaVuelveABajar() {
        BurgerJoint local = local("Igual", "/api/place-photos/vieja.jpg", 2);
        when(repository.conFotoElegidaConUnaReglaVieja(anyInt())).thenReturn(List.of(local));
        when(placesClient.fotosDe(anyString(), any())).thenReturn(java.util.List.of(new FotoElegida("places/x/photos/vieja", "huella-de-places/x/photos/vieja")));

        service.revisarFotos();

        verify(placesClient, never()).downloadPhoto(anyString());
        assertThat(guardado().getPhotoRule()).isEqualTo(EleccionDeFoto.REGLA_DE_FOTO);
    }

    /**
     * Con la cuota de fotos agotada igual se averigua de qué locales no hay ninguna.
     *
     * Antes los dos pasos pedían las dos cuotas antes de empezar, así que la de fotos
     * agotada frenaba también el trabajo que solo necesita la ficha. Y justo eso —saber
     * de cuáles Google no tiene ni una foto— es lo que decide si se los esconde, y no
     * cuesta una sola foto.
     */
    @Test
    void sinCuotaDeFotosIgualAveriguaCualesNoTienenNinguna() {
        when(quotaGuard.canCall(com.hamburguesas.model.PlacesCallType.PHOTO)).thenReturn(false);
        when(repository.findByPhotoUrlIsNull()).thenReturn(List.of(local("Sin nada", null, null)));
        when(placesClient.fotosDe(anyString(), any())).thenReturn(java.util.List.of());

        service.revisarFotos();

        assertThat(guardado().isSinFotosEnGoogle()).isTrue();
    }

    /**
     * Y revisa las que no cambian de foto, que son la mayoría.
     *
     * Quedan anotadas con la regla nueva sin bajar nada, así el mes que viene la cuota
     * de fotos se gasta solo en las que de verdad cambian.
     */
    @Test
    void sinCuotaDeFotosIgualRevisaLasQueNoCambian() {
        when(quotaGuard.canCall(com.hamburguesas.model.PlacesCallType.PHOTO)).thenReturn(false);
        BurgerJoint local = local("Igual", "/api/place-photos/vieja.jpg", 2);
        when(repository.conFotoElegidaConUnaReglaVieja(anyInt())).thenReturn(List.of(local));
        when(placesClient.fotosDe(anyString(), any())).thenReturn(java.util.List.of(new FotoElegida("places/x/photos/vieja", "huella-de-places/x/photos/vieja")));

        service.revisarFotos();

        assertThat(guardado().getPhotoRule()).isEqualTo(EleccionDeFoto.REGLA_DE_FOTO);
    }

    /**
     * La que sí cambia se deja para cuando haya cuota, sin anotarle la regla nueva.
     *
     * Si se la anotara, quedaría fuera de la lista del mes que viene y se habría perdido
     * la mejora sin haber bajado nunca la foto.
     */
    @Test
    void laQueCambiaSinCuotaQuedaParaElMesQueViene() {
        when(quotaGuard.canCall(com.hamburguesas.model.PlacesCallType.PHOTO)).thenReturn(false);
        BurgerJoint local = local("Cambia", "/api/place-photos/vieja.jpg", 2);
        when(repository.conFotoElegidaConUnaReglaVieja(anyInt())).thenReturn(List.of(local));
        when(placesClient.fotosDe(anyString(), any())).thenReturn(java.util.List.of(new FotoElegida("places/x/photos/mejor", "huella-de-places/x/photos/mejor")));

        service.revisarFotos();

        verify(placesClient, never()).downloadPhoto(anyString());
        verify(repository, never()).save(any());
        assertThat(local.getPhotoRule()).isEqualTo(2);
    }

    /**
     * Si la mejor por puntaje resulta ser el logo, se prueba la siguiente.
     *
     * El logo lo sube el local, tiene buen tamaño y suele ser apaisado, así que le gana
     * por puntaje a cualquier fotografía. Solo se descubre mirando los píxeles, o sea
     * después de bajarlo, y para entonces lo único que queda por hacer es seguir.
     */
    @Test
    void siLaPrimeraEsUnLogoSeQuedaConLaSiguiente() throws Exception {
        BurgerJoint local = local("Con logo", null, null);
        when(repository.findByPhotoUrlIsNull()).thenReturn(List.of(local));
        when(placesClient.fotosDe(anyString(), any())).thenReturn(List.of(
            new FotoElegida("places/x/photos/logo", "600x600|El local"),
            new FotoElegida("places/x/photos/la-buena", "4800x3600|Un cliente")));
        when(placesClient.downloadPhoto("places/x/photos/logo")).thenReturn(unLogo());
        when(placesClient.downloadPhoto("places/x/photos/la-buena")).thenReturn(unaFotografia());
        when(photoStorage.save(anyString(), any())).thenReturn("/api/place-photos/buena.jpg");

        service.revisarFotos();

        BurgerJoint despues = guardado();
        assertThat(despues.getPhotoUrl()).isEqualTo("/api/place-photos/buena.jpg");
        assertThat(despues.getPhotoName()).isEqualTo("places/x/photos/la-buena");
    }

    /** Un logo de dos colores, que es de lo que están hechos. */
    private byte[] unLogo() throws Exception {
        BufferedImage img = new BufferedImage(400, 400, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(220, 50, 40));
        g.fillRect(0, 0, 400, 400);
        g.setColor(Color.WHITE);
        g.fillOval(80, 80, 240, 240);
        g.dispose();
        return aBytes(img);
    }

    /** Una fotografía: miles de tonos, que es lo que deja una cámara. */
    private byte[] unaFotografia() throws Exception {
        BufferedImage img = new BufferedImage(400, 400, BufferedImage.TYPE_INT_RGB);
        Random azar = new Random(11);
        for (int y = 0; y < 400; y++) {
            for (int x = 0; x < 400; x++) {
                int base = (x + y) / 4;
                img.setRGB(x, y, new Color(
                    Math.min(255, base + azar.nextInt(70)),
                    Math.min(255, base / 2 + azar.nextInt(70)),
                    azar.nextInt(140)).getRGB());
            }
        }
        return aBytes(img);
    }

    private byte[] aBytes(BufferedImage img) throws Exception {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        ImageIO.write(img, "png", salida);
        return salida.toByteArray();
    }

    /** Y si elige otra, esa se baja y reemplaza a la anterior. */
    @Test
    void siLaReglaNuevaEligeOtraFotoLaReemplaza() {
        BurgerJoint local = local("Cambia", "/api/place-photos/vieja.jpg", 2);
        when(repository.conFotoElegidaConUnaReglaVieja(anyInt())).thenReturn(List.of(local));
        when(placesClient.fotosDe(anyString(), any())).thenReturn(java.util.List.of(new FotoElegida("places/x/photos/mejor", "huella-de-places/x/photos/mejor")));
        when(placesClient.downloadPhoto(anyString())).thenReturn(new byte[] {1, 2, 3});
        when(photoStorage.save(anyString(), any())).thenReturn("/api/place-photos/mejor.jpg");

        service.revisarFotos();

        BurgerJoint despues = guardado();
        assertThat(despues.getPhotoUrl()).isEqualTo("/api/place-photos/mejor.jpg");
        assertThat(despues.getPhotoName()).isEqualTo("places/x/photos/mejor");
        assertThat(despues.getPhotoRule()).isEqualTo(EleccionDeFoto.REGLA_DE_FOTO);
    }
}
