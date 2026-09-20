package com.hamburguesas.exception;

/** La contraseña elegida figura en filtraciones conocidas. */
public class WeakPasswordException extends RuntimeException {
    public WeakPasswordException(String message) {
        super(message);
    }
}
