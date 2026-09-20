package com.hamburguesas.exception;

/**
 * The account exists, but belongs to the other sign-in method. Only raised once the
 * caller has already proved they own the address, so naming the reason leaks nothing.
 */
public class WrongSignInMethodException extends RuntimeException {
    public WrongSignInMethodException(String message) {
        super(message);
    }
}
