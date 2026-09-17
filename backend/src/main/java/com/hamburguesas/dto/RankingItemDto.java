package com.hamburguesas.dto;

public record RankingItemDto(
    Long hamburgueseriaId,
    String nombre,
    String direccion,
    String zona,
    String fotoUrl,
    Double promedio,
    Long cantidadCalificaciones,
    Integer miPuntaje
) {}
