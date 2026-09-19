package com.hamburguesas.exception;

/** The caller went over a rate limit. */
public class TooManyRequestsException extends RuntimeException {
    public TooManyRequestsException(String message) {
        super(message);
    }
}
