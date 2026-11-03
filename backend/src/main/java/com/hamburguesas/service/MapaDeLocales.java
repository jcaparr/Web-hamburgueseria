package com.hamburguesas.service;

import com.hamburguesas.dto.FilaDelMapa;
import com.hamburguesas.dto.FranjaDeLocal;
import com.hamburguesas.dto.HorarioDto;
import com.hamburguesas.dto.LocalDelMapaDto;
import com.hamburguesas.repository.BurgerJointRepository;
import com.hamburguesas.repository.FranjaHorariaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Los locales del mapa (#395). Dos consultas en total, sin importar cuántos locales haya:
 * los locales con su nota, y las franjas de todos. Cero llamadas a Google.
 */
@Service
@RequiredArgsConstructor
public class MapaDeLocales {

    private static final String FOTOS = "/api/place-photos/";

    private final BurgerJointRepository burgerJointRepository;
    private final FranjaHorariaRepository franjaRepository;

    @Transactional(readOnly = true)
    public List<LocalDelMapaDto> locales() {
        Map<Long, List<HorarioDto.Franja>> franjasPorLocal = new HashMap<>();
        for (FranjaDeLocal f : franjaRepository.paraElMapa()) {
            franjasPorLocal.computeIfAbsent(f.burgerJointId(), id -> new ArrayList<>())
                .add(new HorarioDto.Franja(f.dia(), f.abre(), f.cierra()));
        }

        return burgerJointRepository.paraElMapa().stream()
            .map(fila -> aDto(fila, franjasPorLocal.getOrDefault(fila.id(), List.of())))
            .toList();
    }

    private static LocalDelMapaDto aDto(FilaDelMapa fila, List<HorarioDto.Franja> franjas) {
        return new LocalDelMapaDto(fila.id(), fila.placeId(), fila.slug(), fila.name(), fila.area(),
            miniaturaDe(fila.photoUrl()), fila.latitude(), fila.longitude(),
            fila.averageScore(), fila.ratingsCount(), franjas);
    }

    /**
     * La dirección de la foto chica a partir de la grande: "/api/place-photos/X.jpg?v=h"
     * pasa a "/api/place-photos/mini/X.jpg?v=h". Una foto que no es de las nuestras (no
     * debería haber, pero la columna lo permite) se deja como está.
     */
    static String miniaturaDe(String photoUrl) {
        if (photoUrl == null || !photoUrl.startsWith(FOTOS) || photoUrl.startsWith(FOTOS + "mini/")) {
            return photoUrl;
        }
        return FOTOS + "mini/" + photoUrl.substring(FOTOS.length());
    }
}
