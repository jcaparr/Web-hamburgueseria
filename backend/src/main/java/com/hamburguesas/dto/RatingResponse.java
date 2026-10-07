package com.hamburguesas.dto;

import java.time.Instant;

public record RatingResponse(
    Long id,
    Long userId,
    String username,
    /** La hamburguesa del avatar de quien la escribió, o null. */
    String hamburguesa,
    Integer score,
    String comment,
    /** La foto que sacó quien la escribió, o null. */
    String photoUrl,
    Instant createdAt
) {}
