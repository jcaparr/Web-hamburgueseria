package com.hamburguesas.dto;

import java.time.Instant;
import java.util.List;

public record RatingResponse(
    Long id,
    Long userId,
    String username,
    /** La hamburguesa del avatar de quien la escribió, o null. */
    String hamburguesa,
    Integer score,
    String comment,
    /** Las fotos que sacó quien la escribió, en orden: la primera es la portada. */
    List<String> fotos,
    Instant createdAt
) {}
