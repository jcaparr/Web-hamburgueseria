package com.hamburguesas.dto;

import java.time.Instant;

public record RatingResponse(
    Long id,
    Long userId,
    String username,
    Integer score,
    String comment,
    /** La foto que sacó quien la escribió, o null. */
    String photoUrl,
    Instant createdAt
) {}
