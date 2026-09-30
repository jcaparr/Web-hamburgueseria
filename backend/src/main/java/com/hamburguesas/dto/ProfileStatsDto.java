package com.hamburguesas.dto;

public record ProfileStatsDto(
    long ratingsCount,
    long seguidores,
    Double averageScore
) {}
