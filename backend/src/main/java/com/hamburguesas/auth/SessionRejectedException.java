package com.hamburguesas.auth;

/** The refresh token presented is unknown, expired, or already used. */
public class SessionRejectedException extends RuntimeException {
    public SessionRejectedException(String message) {
        super(message);
    }
}
