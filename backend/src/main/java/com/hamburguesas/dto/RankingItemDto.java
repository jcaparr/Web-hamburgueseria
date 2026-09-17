package com.hamburguesas.dto;

public record RankingItemDto(
    Long burgerJointId,
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
