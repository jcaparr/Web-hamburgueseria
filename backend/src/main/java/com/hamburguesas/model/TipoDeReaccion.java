package com.hamburguesas.model;

/**
 * Las reacciones que se le pueden poner a una reseña (#186).
 *
 * Pocas y fijas, pensadas para lo que se dice de una hamburguesa: que dio hambre, que
 * es maravilloso, que encanta, risa y sorpresa —esta última sirve también para una reseña
 * de 1 estrella, donde ninguna de las otras queda bien—. El dibujo lo pone la pantalla.
 *
 * El orden es el del selector, y el que desempata cuando dos tienen la misma cantidad.
 */
public enum TipoDeReaccion {
    HAMBRE,
    MARAVILLOSO,
    ENCANTA,
    RISA,
    SORPRESA
}
