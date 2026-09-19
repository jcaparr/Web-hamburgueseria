package com.hamburguesas.mail;

/** Thrown when an email could not be handed to the SMTP server. */
public class EmailDeliveryException extends RuntimeException {
    public EmailDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
