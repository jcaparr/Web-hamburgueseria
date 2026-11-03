package com.hamburguesas.dto;

/**
 * Una fila de la consulta del mapa, antes de sumarle el horario. Existe porque la consulta
 * no puede traer las franjas en la misma fila: son varias por local.
 */
public record FilaDelMapa(
    Long id,
    String placeId,
    String slug,
    String name,
    String area,
    String photoUrl,
    double latitude,
    double longitude,
    Double averageScore,
    long ratingsCount
) {}
