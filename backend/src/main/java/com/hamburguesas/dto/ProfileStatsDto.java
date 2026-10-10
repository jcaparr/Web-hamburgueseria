package com.hamburguesas.dto;

/** @param siguiendo a cuántas personas sigue: se ve en el perfil propio, con su lista (#232) */
public record ProfileStatsDto(
    long ratingsCount,
    long seguidores,
    long siguiendo,
    Double averageScore
) {}
