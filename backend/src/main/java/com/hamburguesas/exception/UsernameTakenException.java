package com.hamburguesas.exception;

/**
 * El nombre que pidió ya es de otro, o está reservado.
 *
 * Va con código propio porque el frontend no tiene que mostrar un cartel sino marcar
 * ese campo en rojo, que es donde la persona puede arreglarlo.
 */
public class UsernameTakenException extends RuntimeException {
    public UsernameTakenException(String message) {
        super(message);
    }
}
