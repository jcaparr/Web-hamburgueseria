package com.hamburguesas.exception;

import lombok.Getter;

/**
 * Entró con Google por primera vez y todavía no eligió cómo lo van a encontrar.
 *
 * No es un error de quien llama: es el paso que falta. La cuenta no se crea hasta que
 * vuelva con un nombre, así que hasta entonces acá no pasó nada.
 */
@Getter
public class NeedsUsernameException extends RuntimeException {

    /** Uno libre, sacado de su email, para que no tenga que inventar de cero. */
    private final String suggestion;

    public NeedsUsernameException(String suggestion) {
        super("Elegí un nombre de usuario para terminar de crear tu cuenta");
        this.suggestion = suggestion;
    }
}
