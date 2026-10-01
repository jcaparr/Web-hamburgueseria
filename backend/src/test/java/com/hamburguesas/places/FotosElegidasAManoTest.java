package com.hamburguesas.places;

import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.repository.BurgerJointRepository;
import com.hamburguesas.repository.RatingRepository;
import com.hamburguesas.repository.SavedTourRepository;
import com.hamburguesas.repository.WishlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
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
 * La foto de portada elegida a mano.
 *
 * La regla sabe elegir una foto bien sacada, pero no sabe qué muestra: entre la fachada
 * del local y una bandeja de empanadas no hay nada en los datos que las distinga, porque
 * las dos son fotografías de tamaño parecido subidas por el local. Eso solo se arregla
 * mirando, y lo que se mira una vez tiene que quedar escrito y surtir efecto.
 *
 * Lo que se prueba acá es que la elección gane, que llegue a aplicarse, y que no se
 * aplique más de una vez: cada aplicación cuesta llamadas a Google.
 */
class FotosElegidasAManoTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    /**
     * Un local cuya mejor foto por puntaje no es la que queremos.
     *
     * La primera es apaisada, grande y la subió el local: gana por lejos. Es el afiche de
     * empanadas. La segunda es la hamburguesa, subida por alguien que fue a comer.
     */
    private static final String DIEZ_FOTOS = """
        {
          "id": "ChIJkeke",
          "displayName": { "text": "Keke & Larry" },
          "photos": [
            { "name": "places/ChIJkeke/photos/afiche", "widthPx": 2048, "heightPx": 1536,
              "authorAttributions": [{ "displayName": "Keke & Larry" }] },
            { "name": "places/ChIJkeke/photos/hamburguesa", "widthPx": 1200, "heightPx": 900,
              "authorAttributions": [{ "displayName": "Un cliente" }] }
          ]
        }
        """;

    private static final String HUELLA_DE_LA_HAMBURGUESA = "1200x900|Un cliente";
    private static final String HUELLA_DEL_AFICHE = "2048x1536|Keke & Larry";

    private static JsonNode json(String raw) {
        return MAPPER.readTree(raw);
    }

    // ---- la elección ----

    /** Sin anotar nada gana el afiche, que es el problema que esto viene a resolver. */
    @Test
    void sinElegirNadaGanaLaQueDiceLaRegla() {
        List<FotoElegida> fotos = EleccionDeFoto.mejoresFotos(json(DIEZ_FOTOS));

        assertThat(fotos.get(0).huella()).isEqualTo(HUELLA_DEL_AFICHE);
    }

    /** Elegida a mano gana, por más que la regla la haya dejado atrás. */
    @Test
    void laElegidaAManoVaPrimera() {
        List<FotoElegida> fotos =
            EleccionDeFoto.mejoresFotos(json(DIEZ_FOTOS), HUELLA_DE_LA_HAMBURGUESA);

        assertThat(fotos.get(0).huella()).isEqualTo(HUELLA_DE_LA_HAMBURGUESA);
        assertThat(fotos.get(0).name()).isEqualTo("places/ChIJkeke/photos/hamburguesa");
    }

    /** Las otras quedan atrás, que es lo que hace falta si la elegida no se puede bajar. */
    @Test
    void lasDemasQuedanAtrasYNoSeRepiten() {
        List<FotoElegida> fotos =
            EleccionDeFoto.mejoresFotos(json(DIEZ_FOTOS), HUELLA_DE_LA_HAMBURGUESA);

        assertThat(fotos).extracting(FotoElegida::huella)
            .containsExactly(HUELLA_DE_LA_HAMBURGUESA, HUELLA_DEL_AFICHE);
    }

    /**
     * Una huella que no aparece entre las fotos del local no rompe nada: manda la regla.
     *
     * Pasa si borran esa foto de Google, o si se copió mal. Dejar al local sin portada
     * sería castigar al visitante por un error de configuración nuestro.
     */
    @Test
    void siLaHuellaAnotadaNoEstaMandaLaRegla() {
        List<FotoElegida> fotos =
            EleccionDeFoto.mejoresFotos(json(DIEZ_FOTOS), "1x1|Nadie");

        assertThat(fotos.get(0).huella()).isEqualTo(HUELLA_DEL_AFICHE);
    }

    /** También al entrar por una búsqueda: si no, se baja una foto para cambiarla después. */
    @Test
    void tambienValeParaLosLocalesQueTraeLaBusqueda() {
        JsonNode respuesta = json("{ \"places\": [" + DIEZ_FOTOS + "] }");

        PlacesSearchResult resultado =
            PlacesClient.parse(respuesta, Map.of("ChIJkeke", HUELLA_DE_LA_HAMBURGUESA));

        assertThat(resultado.places().get(0).photoFingerprint())
            .isEqualTo(HUELLA_DE_LA_HAMBURGUESA);
    }

    // ---- que la elección llegue a aplicarse ----

    private PlacesProperties properties;
    private PlacesClient placesClient;
    private BurgerJointRepository repository;
    private PhotoStorage photoStorage;
    private PlacesSyncService service;

    @BeforeEach
    void setUp() {
        properties = new PlacesProperties();
        properties.setApiKey("clave-de-prueba");
        properties.getSync().setDelayBetweenCallsMs(0);

        placesClient = mock(PlacesClient.class);
        repository = mock(BurgerJointRepository.class);
        photoStorage = mock(PhotoStorage.class);
        PlacesQuotaGuard quotaGuard = mock(PlacesQuotaGuard.class);

        when(quotaGuard.canCall(any())).thenReturn(true);
        when(repository.findByPhotoUrlIsNull()).thenReturn(List.of());
        when(repository.findByPhotoUrlIsNotNull()).thenReturn(List.of());
        when(repository.conFotoElegidaConUnaReglaVieja(anyInt())).thenReturn(List.of());
        when(repository.findByPlaceId(anyString())).thenReturn(Optional.empty());
        when(repository.nombresDeRubro(anyString())).thenReturn(List.of());
        when(photoStorage.save(anyString(), any())).thenReturn("/api/place-photos/elegida.jpg");

        service = new PlacesSyncService(
            properties, placesClient, quotaGuard, photoStorage, repository, new Barrios(),
            new Zonas(new Barrios(), properties), mock(RatingRepository.class),
            mock(WishlistRepository.class), mock(SavedTourRepository.class),
            new FastFoodMarker(repository, properties));
    }

    /** Un local con la foto de siempre puesta y una elegida a mano distinta. */
    private BurgerJoint keke() {
        return BurgerJoint.builder()
            .id(1L).placeId("ChIJkeke").name("Keke & Larry")
            .address("Av. del Libertador 5670").area("Belgrano")
            .photoUrl("/api/place-photos/afiche.jpg")
            .photoName("places/ChIJkeke/photos/afiche")
            .photoFingerprint(HUELLA_DEL_AFICHE)
            .photoRule(EleccionDeFoto.REGLA_DE_FOTO)
            .build();
    }

    private BurgerJoint guardado() {
        ArgumentCaptor<BurgerJoint> capturado = ArgumentCaptor.forClass(BurgerJoint.class);
        verify(repository, atLeastOnce()).save(capturado.capture());
        return capturado.getValue();
    }

    /**
     * El local ya está marcado con la regla al día, así que la revisión de siempre no lo
     * mira. Sin esto, anotar la foto en la configuración no cambiaría nada.
     */
    @Test
    void seAplicaAunqueElLocalYaEsteAlDiaConLaRegla() {
        properties.getSync().setFotosElegidas(Map.of("ChIJkeke", HUELLA_DE_LA_HAMBURGUESA));
        when(repository.findByPlaceIdIn(any())).thenReturn(List.of(keke()));
        when(placesClient.fotosDe("ChIJkeke")).thenReturn(List.of(
            new FotoElegida("places/ChIJkeke/photos/hamburguesa", HUELLA_DE_LA_HAMBURGUESA)));
        when(placesClient.downloadPhoto(anyString())).thenReturn(new byte[] {1, 2, 3});

        service.revisarFotos();

        assertThat(guardado().getPhotoFingerprint()).isEqualTo(HUELLA_DE_LA_HAMBURGUESA);
    }

    /**
     * Y una vez aplicada no se vuelve a pedir la ficha.
     *
     * Si no, cada local anotado costaría una llamada por sincronización para siempre, y
     * anotar fotos saldría cada vez más caro.
     */
    @Test
    void unaVezPuestaNoSeVuelveAPreguntar() {
        BurgerJoint yaPuesta = keke();
        yaPuesta.setPhotoFingerprint(HUELLA_DE_LA_HAMBURGUESA);
        properties.getSync().setFotosElegidas(Map.of("ChIJkeke", HUELLA_DE_LA_HAMBURGUESA));
        when(repository.findByPlaceIdIn(any())).thenReturn(List.of(yaPuesta));

        service.revisarFotos();

        verify(placesClient, never()).fotosDe(anyString());
    }

    /**
     * Una foto elegida a mano que parece un logo se baja igual.
     *
     * La detección de logos mira los píxeles y acierta sobre lo que la imagen es, pero no
     * sobre lo que se quiso: si alguien eligió esa foto, descartarla deshace la elección
     * en silencio y deja puesta la que la regla prefería. Un local con la marca bien
     * fotografiada en la puerta es una portada legítima.
     */
    @Test
    void unaElegidaAManoNoSeDescartaPorParecerUnLogo() {
        properties.getSync().setFotosElegidas(Map.of("ChIJkeke", HUELLA_DE_LA_HAMBURGUESA));
        when(repository.findByPlaceIdIn(any())).thenReturn(List.of(keke()));
        when(placesClient.fotosDe("ChIJkeke")).thenReturn(List.of(
            new FotoElegida("places/ChIJkeke/photos/hamburguesa", HUELLA_DE_LA_HAMBURGUESA)));
        when(placesClient.downloadPhoto(anyString())).thenReturn(unLogo());

        service.revisarFotos();

        assertThat(guardado().getPhotoUrl()).isEqualTo("/api/place-photos/elegida.jpg");
    }

    /** Lo mismo pero sin elegir a mano: ahí sí el logo se descarta, como siempre. */
    @Test
    void sinElegirAManoElLogoSeSigueDescartando() {
        BurgerJoint local = keke();
        local.setPhotoRule(1);
        when(repository.conFotoElegidaConUnaReglaVieja(anyInt())).thenReturn(List.of(local));
        when(placesClient.fotosDe("ChIJkeke")).thenReturn(List.of(
            new FotoElegida("places/ChIJkeke/photos/otra", "999x999|Otro")));
        when(placesClient.downloadPhoto(anyString())).thenReturn(unLogo());

        service.revisarFotos();

        verify(photoStorage, never()).save(anyString(), any());
    }

    /**
     * Dos colores planos, que es lo que distingue a un logo de una fotografía.
     *
     * El mismo criterio que usa EsUnaFotografia: contar colores distintos. Una foto
     * sacada con una cámara pasa los seiscientos; esto tiene dos.
     */
    private static byte[] unLogo() {
        BufferedImage imagen = new BufferedImage(400, 300, BufferedImage.TYPE_INT_RGB);
        Graphics2D pincel = imagen.createGraphics();
        pincel.setColor(Color.WHITE);
        pincel.fillRect(0, 0, 400, 300);
        pincel.setColor(Color.RED);
        pincel.fillRect(50, 50, 200, 100);
        pincel.dispose();

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try {
            ImageIO.write(imagen, "png", salida);
        } catch (java.io.IOException ex) {
            throw new IllegalStateException(ex);
        }
        return salida.toByteArray();
    }
}
