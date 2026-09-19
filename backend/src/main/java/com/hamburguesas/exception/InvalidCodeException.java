package com.hamburguesas.exception;

/** A verification code that is wrong, expired, already used, or out of attempts. */
public class InvalidCodeException extends RuntimeException {
    public InvalidCodeException(String message) {
        super(message);
    }
}
