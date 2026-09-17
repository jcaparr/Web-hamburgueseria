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

    @GetMapping("/{fileName}")
    public ResponseEntity<Resource> get(@PathVariable String fileName) {
        var path = photoStorage.resolve(fileName);
        if (!Files.isRegularFile(path)) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
            .contentType(MediaType.IMAGE_JPEG)
            .cacheControl(org.springframework.http.CacheControl.maxAge(java.time.Duration.ofDays(30)))
            .body(new FileSystemResource(path));
    }
}
