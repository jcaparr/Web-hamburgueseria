package com.hamburguesas.dto;

import com.hamburguesas.model.ModoDeViaje;

import java.time.Instant;
import java.util.List;

/** Un recorrido guardado, como se ve en el perfil. */
public record SavedTourDto(
    Long id,
    String name,
    double kilometros,
    int minutos,
    ModoDeViaje modo,
    Instant creadoEl,
    List<TourStopDto> paradas
) {}
