package com.hamburguesas.places;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;

/** Serves the photos the sync job already downloaded from Google, so the API key never reaches the browser. */
@RestController
@RequestMapping("/api/place-photos")
@RequiredArgsConstructor
public class PlacePhotoController {

    private final PhotoStorage photoStorage;
    private final MiniaturasDeFotos miniaturas;

    /**
     * La foto chica para el mapa (#395). Sin miniatura posible —un WebP, por ejemplo— se
     * sirve la foto entera: un punto con una foto pesada es mejor que un punto roto.
     */
    @GetMapping("/mini/{fileName}")
    public ResponseEntity<Resource> miniatura(@PathVariable String fileName) {
        var mini = miniaturas.de(fileName).orElse(null);
        if (mini == null) {
            return get(fileName);
        }
        return ResponseEntity.ok()
            .contentType(MediaType.IMAGE_JPEG)
            .cacheControl(org.springframework.http.CacheControl.maxAge(java.time.Duration.ofDays(30)))
            .body(new FileSystemResource(mini));
    }

    @GetMapping("/{fileName}")
    public ResponseEntity<Resource> get(@PathVariable String fileName) {
        var path = photoStorage.resolve(fileName).orElse(null);
        if (path == null || !Files.isRegularFile(path)) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
            // El tipo sale de la extensión, que al guardar se eligió mirando los bytes.
            .contentType(MediaType.parseMediaType(ImageFormat.porExtension(fileName).mediaType()))
            .cacheControl(org.springframework.http.CacheControl.maxAge(java.time.Duration.ofDays(30)))
            .body(new FileSystemResource(path));
    }
}
