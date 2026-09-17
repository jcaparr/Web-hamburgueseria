package com.hamburguesas.dto;

import java.time.Instant;

public record CalificacionResponse(
    Long id,
    Long usuarioId,
    String usuarioNombre,
    Integer puntaje,
    String comentario,
    Instant fecha
) {}
