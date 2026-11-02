package com.hamburguesas.exception;

import java.time.LocalDate;

/**
 * Cambió su nombre hace menos de 30 días (#380).
 *
 * Lleva el día en que puede volver a hacerlo, para que la pantalla lo diga en vez de
 * solo negar. Es un día de Buenos Aires: desde las 00:00 de ese día ya puede.
 */
public class UsernameCooldownException extends RuntimeException {

    private final LocalDate puedeCambiarDesde;

    public UsernameCooldownException(String message, LocalDate puedeCambiarDesde) {
        super(message);
        this.puedeCambiarDesde = puedeCambiarDesde;
    }

    public LocalDate getPuedeCambiarDesde() {
        return puedeCambiarDesde;
    }
}
