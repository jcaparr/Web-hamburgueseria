package com.hamburguesas.model;

/**
 * De quién son las reseñas que se están mirando.
 *
 * Son dos pestañas y no un filtro con muchas opciones porque son dos intenciones
 * distintas: enterarse de lo último que pasó en la ciudad, o ver qué anduvo comiendo
 * la gente que te interesa.
 */
public enum FuenteDelFeed {
    /** Lo último de toda la app, que es lo que se ve sin seguir a nadie. */
    TODOS,
    /** Solo de quienes seguís. */
    SIGUIENDO
}
