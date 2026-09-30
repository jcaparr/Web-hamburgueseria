package com.hamburguesas.fotos;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.time.Duration;

/**
 * Sirve las fotos que la gente subió con sus reseñas.
 *
 * Siempre como JPEG, sin mirar el archivo: al guardarlas se reescribieron todas a ese
 * formato, así que acá no hay nada que adivinar. Anunciar un tipo fijo es también lo
 * que asegura que nada de lo que alguien haya subido pueda terminar sirviéndose como
 * HTML o como script.
 */
@RestController
@RequestMapping("/api/rating-photos")
@RequiredArgsConstructor
public class FotoDeReseniaController {

    private final FotosDeResenias fotos;

    @GetMapping("/{fileName}")
    public ResponseEntity<Resource> get(@PathVariable String fileName) {
        var path = fotos.resolve(fileName).orElse(null);
        if (path == null || !Files.isRegularFile(path)) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
            .contentType(MediaType.IMAGE_JPEG)
            // El nombre es un UUID que no se reusa, así que el archivo de una URL nunca
            // cambia: se puede cachear todo lo que el navegador quiera.
            .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic())
            .header("X-Content-Type-Options", "nosniff")
            .body(new FileSystemResource(path));
    }
}
