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
        if (!looksLikeImage(bytes)) {
            log.warn("Lo que llegó para el lugar {} no es una imagen ({} bytes), no se guarda",
                placeId, bytes.length);
            return null;
        }

        String fileName = sanitize(placeId) + ".jpg";
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

    /**
     * Mira los primeros bytes, que en los formatos de imagen son una firma fija. No
     * valida que la imagen esté entera, pero alcanza para distinguir una foto de un
     * error o un JSON, que es de lo que se trata.
     */
    private boolean looksLikeImage(byte[] bytes) {
        if (bytes.length < 12) {
            return false;
        }
        boolean jpeg = (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8;
        boolean png = (bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G';
        boolean gif = bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F';
        boolean webp = bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
            && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
        return jpeg || png || gif || webp;
    }

    public Path resolve(String fileName) {
        return Paths.get(properties.getPhotos().getDirectory()).resolve(sanitize(fileName));
    }

    /** Place ids are opaque Google strings, so strip anything that could escape the directory. */
    private String sanitize(String value) {
        return value.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
