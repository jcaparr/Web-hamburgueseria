package com.hamburguesas.dto;

/** Una parada del recorrido. */
public record TourStopDto(
    int orden,
    /**
     * Cuánto hay que caminar desde la parada anterior. En la primera es lo que hay
     * desde donde arranca quien camina, o cero si no dijo dónde está.
     */
    double kilometros,
    /** Si esta persona ya la puntuó. Sin sesión es siempre falso. */
    boolean visitada,
    BurgerJointDto local
) {}
