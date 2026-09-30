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
    /**
     * El promedio de la hamburguesería, que es otra cosa que la nota de esta reseña.
     *
     * Van las dos en la tarjeta a propósito: una dice qué le pareció a esta persona y
     * la otra qué le parece a todo el mundo, y verlas juntas es lo que deja saber si
     * estás leyendo una opinión que se sale de la norma.
     */
    Double promedioDelLocal,
    Integer score,
    String comment,
    /** La foto que sacó quien la escribió, o null. */
    String fotoDeLaResenia,
    Instant createdAt,
    boolean editada
) {}
