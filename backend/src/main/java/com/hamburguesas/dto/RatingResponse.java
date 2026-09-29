package com.hamburguesas.dto;

import java.time.Instant;

public record RatingResponse(
    Long id,
    Long userId,
    String username,
    Integer score,
    String comment,
    Instant createdAt
) {}
