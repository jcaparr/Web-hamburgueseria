package com.hamburguesas.dto;

public record BurgerJointDto(
    Long id,
    String name,
    String address,
    String area,
    String photoUrl,
    Double latitude,
    Double longitude,
    Double averageScore,
    Long ratingsCount,
    boolean inWishlist
) {}
