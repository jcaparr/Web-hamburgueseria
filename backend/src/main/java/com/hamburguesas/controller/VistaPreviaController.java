package com.hamburguesas.controller;

import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.places.PhotoStorage;
import com.hamburguesas.repository.BurgerJointRepository;
import com.hamburguesas.service.FichaDeLocal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Locale;

/**
 * La página que ven los lectores de vistas previas (WhatsApp, Instagram, X…) cuando
 * alguien comparte la ficha de un local (#277).
 *
 * Esos lectores no ejecutan la app: solo leen las etiquetas del HTML, y el index.html es
 * el mismo para cualquier dirección, así que todos veían el logo de Burgómetro. Caddy les
 * manda a ellos —y solo a ellos— esta página, con el nombre, la nota y la foto del local.
 * Las personas siguen recibiendo la app.
 *
 * No llama a Google: la foto ya está guardada en el servidor y la nota sale de las
 * reseñas propias.
 */
@RestController
@RequestMapping("/api/burger-joints")
public class VistaPreviaController {

    /** La imagen de siempre, para los locales sin foto o con una que WhatsApp no levantaría. */
    private static final String IMAGEN_DEL_SITIO = "/og.png";

    /**
     * Lo más que pesa una foto para ofrecerla como vista previa. WhatsApp descarta las
     * imágenes de más o menos medio mega y muestra el enlace pelado: mejor el logo.
     */
    static final long PESO_MAXIMO_DE_FOTO = 300 * 1024;

    private static final Locale ES_AR = Locale.forLanguageTag("es-AR");

    private final BurgerJointRepository burgerJointRepository;
    private final FichaDeLocal fichaDeLocal;
    private final PhotoStorage photoStorage;
    private final String sitio;

    public VistaPreviaController(BurgerJointRepository burgerJointRepository,
                                 FichaDeLocal fichaDeLocal,
                                 PhotoStorage photoStorage,
                                 @Value("${app.sitio.url}") String sitio) {
        this.burgerJointRepository = burgerJointRepository;
        this.fichaDeLocal = fichaDeLocal;
        this.photoStorage = photoStorage;
        // Sin la barra final: los caminos ya empiezan con una.
        this.sitio = sitio.replaceAll("/+$", "");
    }

    @GetMapping("/{id}/vista-previa")
    public ResponseEntity<String> vistaPrevia(@PathVariable Long id) {
        // Un 404 pelado y no la excepción de siempre: esa responde JSON, y un lector que
        // pide solo text/html lo transformaba en un 500.
        BurgerJoint local = burgerJointRepository.findById(id).orElse(null);
        if (local == null) {
            return ResponseEntity.notFound().build();
        }
        var ficha = fichaDeLocal.para(local, null);

        String enlace = sitio + "/burger-joints/" + id;
        String descripcion = descripcion(local.getName(), local.getArea(),
            ficha.averageScore(), ficha.ratingsCount());
        String imagen = sitio + imagenDe(local);

        // Las etiquetas son lo único que importa; el resto es para quien llegue acá por
        // error (sin la app no hay otra cosa que mostrarle más que el enlace al local).
        String html = """
            <!doctype html>
            <html lang="es-AR">
            <head>
            <meta charset="UTF-8">
            <title>%1$s · Burgómetro</title>
            <meta name="description" content="%2$s">
            <link rel="canonical" href="%3$s">
            <meta property="og:type" content="website">
            <meta property="og:site_name" content="Burgómetro">
            <meta property="og:locale" content="es_AR">
            <meta property="og:title" content="%1$s">
            <meta property="og:description" content="%2$s">
            <meta property="og:url" content="%3$s">
            <meta property="og:image" content="%4$s">
            <meta property="og:image:alt" content="Foto de %1$s">
            <meta name="twitter:card" content="summary_large_image">
            <meta name="twitter:title" content="%1$s">
            <meta name="twitter:description" content="%2$s">
            <meta name="twitter:image" content="%4$s">
            </head>
            <body>
            <p><a href="%3$s">%1$s en Burgómetro</a></p>
            </body>
            </html>
            """.formatted(escapar(local.getName()), escapar(descripcion), escapar(enlace),
            escapar(imagen));

        return ResponseEntity.ok()
            // Los lectores lo piden una vez por enlace compartido; unos minutos alcanzan
            // para que una nota nueva aparezca pronto sin armar la página en cada pedido.
            .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic())
            .contentType(new MediaType("text", "html", java.nio.charset.StandardCharsets.UTF_8))
            .body(html);
    }

    /** Misma idea que la descripción de la ficha para los buscadores (descripcionDe en la web). */
    static String descripcion(String nombre, String barrio, Double promedio, Long cantidad) {
        String donde = barrio == null || barrio.isBlank() ? "Buenos Aires" : barrio;
        if (promedio != null && cantidad != null && cantidad > 0) {
            String resenias = cantidad == 1 ? "1 reseña" : cantidad + " reseñas";
            return String.format(ES_AR, "Nota %.1f de 5 con %s en Burgómetro. %s.", promedio, resenias, donde);
        }
        return "Hamburguesería en " + donde + ". Mirá dónde queda, su horario y las reseñas en Burgómetro.";
    }

    /** El camino de la foto del local si es chica y existe, o el de la imagen del sitio. */
    private String imagenDe(BurgerJoint local) {
        String foto = local.getPhotoUrl();
        if (foto == null || !foto.startsWith("/api/place-photos/")) {
            return IMAGEN_DEL_SITIO;
        }
        String sinVersion = foto.contains("?") ? foto.substring(0, foto.indexOf('?')) : foto;
        String archivo = sinVersion.substring("/api/place-photos/".length());
        try {
            var ruta = photoStorage.resolve(archivo).orElse(null);
            if (ruta != null && Files.isRegularFile(ruta) && Files.size(ruta) <= PESO_MAXIMO_DE_FOTO) {
                return foto;
            }
        } catch (IOException ex) {
            // Sin poder medirla, la del sitio.
        }
        return IMAGEN_DEL_SITIO;
    }

    /** El nombre lo cargan Google o la gente: nunca entra al HTML sin escapar. */
    static String escapar(String texto) {
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
