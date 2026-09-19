package com.hamburguesas.exception;

/**
 * The password was right but the account was never activated. Raised only after the
 * password check, so it never tells a stranger which emails are registered.
 */
public class EmailNotVerifiedException extends RuntimeException {
    public EmailNotVerifiedException(String message) {
        super(message);
    }
}
