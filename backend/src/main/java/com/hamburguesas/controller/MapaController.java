package com.hamburguesas.controller;

import com.hamburguesas.dto.LocalDelMapaDto;
import com.hamburguesas.service.MapaDeLocales;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

/**
 * Los locales del mapa (#395). Público como el resto de los locales, y con caché: la lista
 * cambia cuando se suma un local o llega una reseña, y cinco minutos de atraso en una nota
 * no le importan a nadie. Así el mapa no hace trabajo por cada visita.
 */
@RestController
@RequestMapping("/api/burger-joints/mapa")
@RequiredArgsConstructor
public class MapaController {

    private final MapaDeLocales mapaDeLocales;

    @GetMapping
    public ResponseEntity<List<LocalDelMapaDto>> locales() {
        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic())
            .body(mapaDeLocales.locales());
    }
}
