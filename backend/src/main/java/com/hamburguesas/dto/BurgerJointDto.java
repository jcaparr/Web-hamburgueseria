package com.hamburguesas.dto;

public record BurgerJointDto(
    Long id,
    /**
     * El identificador del local en Google. Es público —viaja en cualquier enlace a
     * Maps— y es lo que permite abrir la ficha del local en vez de un pin sin nombre.
     */
    String placeId,
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
