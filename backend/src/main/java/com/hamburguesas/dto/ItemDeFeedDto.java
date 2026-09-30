package com.hamburguesas.dto;

import java.time.Instant;

/**
 * Una reseña como se lee en el feed.
 *
 * Trae junto lo de las tres partes —quién, dónde y qué dijo— porque una tarjeta del
 * feed las muestra todas, y pedirlas por separado sería una consulta por tarjeta.
 *
 * La fecha es la de cuando se escribió, no la de la última edición. Si se editó, se
 * dice al lado; pero la reseña sigue siendo de ese día.
 */
public record ItemDeFeedDto(
    Long ratingId,
    Long autorId,
    String autorUsername,
    Long burgerJointId,
    String burgerJointName,
    String photoUrl,
    String area,
    Integer score,
    String comment,
    /** La foto que sacó quien la escribió, o null. */
    String fotoDeLaResenia,
    Instant createdAt,
    boolean editada
) {}
