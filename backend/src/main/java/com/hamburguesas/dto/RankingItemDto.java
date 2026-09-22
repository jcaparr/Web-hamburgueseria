package com.hamburguesas.dto;

public record RankingItemDto(
    Long burgerJointId,
    /** Ver el comentario en BurgerJointDto: sirve para abrir la ficha en Maps. */
    String placeId,
    String name,
    String address,
    String area,
    String photoUrl,
    Double latitude,
    Double longitude,
    Double averageScore,
    Long ratingsCount,
    Integer myScore
) {}
