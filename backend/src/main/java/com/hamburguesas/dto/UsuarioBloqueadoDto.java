package com.hamburguesas.dto;

import java.time.Instant;

/**
 * Alguien a quien bloqueaste, como aparece en la lista para desbloquearlo.
 *
 * Es el único lugar de la app donde vuelve a verse ese nombre, porque en todos los
 * demás quedó escondido. Por eso lleva la fecha: sin ella, una lista de nombres que no
 * se reconocen no le dice a nadie qué pasó ahí.
 */
public record UsuarioBloqueadoDto(
    Long userId,
    String username,
    Instant bloqueadoEl
) {}
