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

    public Path resolve(String fileName) {
        return Paths.get(properties.getPhotos().getDirectory()).resolve(sanitize(fileName));
    }

    /** Place ids are opaque Google strings, so strip anything that could escape the directory. */
    private String sanitize(String value) {
        return value.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
