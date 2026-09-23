package com.hamburguesas.places;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
            return "/api/place-photos/" + fileName;
        } catch (IOException ex) {
            log.warn("Could not store photo for place {}: {}", placeId, ex.getMessage());
            return null;
        }
    }

    public Path resolve(String fileName) {
        return Paths.get(properties.getPhotos().getDirectory()).resolve(sanitize(fileName));
    }

    /** Place ids are opaque Google strings, so strip anything that could escape the directory. */
    private String sanitize(String value) {
        return value.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
