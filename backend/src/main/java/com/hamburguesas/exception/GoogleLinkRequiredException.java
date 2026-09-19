package com.hamburguesas.exception;

/**
 * Signing in with Google landed on an email that already has an account here. Not an
 * error the user did anything wrong to cause: the frontend turns it into the screen
 * that asks for the confirmation code.
 */
public class GoogleLinkRequiredException extends RuntimeException {
    public GoogleLinkRequiredException(String message) {
        super(message);
    }
}
