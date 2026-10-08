package com.hamburguesas.model;

/**
 * Las reacciones que se le pueden poner a una reseña (#186).
 *
 * Pocas y fijas, pensadas para lo que se dice de una hamburguesa: que dio hambre, que
 * está de fuego, aplausos, risa y sorpresa —esta última sirve también para una reseña
 * de 1 estrella, donde ninguna de las otras queda bien—. El emoji lo pone la pantalla.
 *
 * El orden es el del selector, y el que desempata cuando dos tienen la misma cantidad.
 */
public enum TipoDeReaccion {
    HAMBRE,
    FUEGO,
    APLAUSO,
    RISA,
    SORPRESA
}
