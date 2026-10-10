package com.hamburguesas.places;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Stores Google photos on disk once, at sync time. Serving them ourselves keeps the API key
 * out of the browser and means a photo costs one Google call ever, not one per page view.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PhotoStorage {

    private final PlacesProperties properties;

    /** @return the public path to store on the burger joint, or null if it could not be saved. */
    public String save(String placeId, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }

        // Guardar lo que venga es lo que dejó 345 archivos de 700 bytes con un JSON
        // adentro, que en la app se veían como imágenes rotas y que nadie iba a
        // reintentar, porque el local ya figuraba "con foto".
        ImageFormat formato = ImageFormat.of(bytes);
        if (formato == null) {
            log.warn("Lo que llegó para el lugar {} no es una imagen ({} bytes), no se guarda",
                placeId, bytes.length);
            return null;
        }

        // La extensión sale de los bytes y no de una suposición: Google devuelve lo que
        // el local subió, y una de cada ocho fotos es PNG. Guardarlas todas como ".jpg"
        // dejó 51 archivos anunciando un formato que no era el suyo.
        String fileName = sanitize(placeId) + "." + formato.extension();
        try {
            Path directory = Paths.get(properties.getPhotos().getDirectory());
            Files.createDirectories(directory);
            Files.write(directory.resolve(fileName), bytes);
            return "/api/place-photos/" + fileName + "?v=" + version(bytes);
        } catch (IOException ex) {
            log.warn("Could not store photo for place {}: {}", placeId, ex.getMessage());
            return null;
        }
    }

    /**
     * El archivo de esa foto, o vacío si el nombre apunta afuera del directorio.
     *
     * El nombre viene de la URL —/api/place-photos/{fileName}— así que lo escribe quien
     * pide. sanitize() tapa las barras, pero deja pasar el punto porque hace falta para
     * la extensión, y con eso ".." sobrevive entero. Lo único que frenaba un
     * /api/place-photos/../../application.yml era que Spring rechaza esos segmentos
     * antes de llegar acá: una protección que no es de este código y que una versión o
     * una configuración distinta puede aflojar.
     *
     * Así que se comprueba acá, que es donde se abre el archivo: se normaliza la ruta y
     * se exige que siga colgando del directorio de fotos.
     */
    public Optional<Path> resolve(String fileName) {
        Path directorio = Paths.get(properties.getPhotos().getDirectory())
            .toAbsolutePath().normalize();
        Path archivo = directorio.resolve(sanitize(fileName)).normalize();

        if (!archivo.startsWith(directorio)) {
            log.warn("Se pidió una foto con un nombre que apunta afuera del directorio");
            return Optional.empty();
        }
        return Optional.of(archivo);
    }

    /**
     * Una marca de esta foto en particular, para la dirección (#229).
     *
     * El archivo se llama siempre igual, el identificador del local, y se sirve con 30
     * días de caché. Cuando se cambiaba la portada, quien ya había visto el local seguía
     * viendo la vieja hasta un mes: el navegador no tenía por qué volver a pedirla. Con
     * la marca, una foto nueva es una dirección nueva, y la caché larga sigue sirviendo
     * para las que no cambian.
     *
     * Sale de los bytes y no de la fecha: la misma foto bajada dos veces da la misma
     * dirección, y no obliga a nadie a volver a bajarla.
     */
    private static String version(byte[] bytes) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(bytes);
            return HexFormat.of().formatHex(hash, 0, 5);
        } catch (NoSuchAlgorithmException ex) {
            // SHA-256 está en toda JVM: esto no pasa.
            throw new IllegalStateException(ex);
        }
    }

    /** Place ids are opaque Google strings, so strip anything that could escape the directory. */
    private String sanitize(String value) {
        return value.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
