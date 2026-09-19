package com.hamburguesas.auth;

import com.hamburguesas.exception.TooManyRequestsException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * The per-account limits, in one place so the password flows and the Google flow
 * share a budget. Kept separate they would each allow the full quota, which for an
 * attacker is simply two doors instead of one.
 */
@Component
@RequiredArgsConstructor
public class AuthRateLimits {

    private final RateLimiter rateLimiter;
    private final AuthProperties properties;

    /**
     * Caps how many emails one address can be made to receive, whoever asks and from
     * wherever. Without it, anyone could use registration or "forgot password" to bury
     * someone else's inbox, and burn our daily sending quota doing it.
     */
    public void requireSendAllowance(String email) {
        var config = properties.getRateLimit();
        if (!rateLimiter.tryAcquire("send:" + email, config.getPerEmailSends(),
                Duration.ofMinutes(config.getPerEmailSendWindowMinutes()))) {
            throw new TooManyRequestsException(
                "Ya te mandamos varios emails. Esperá un rato antes de pedir otro.");
        }
    }

    /**
     * Caps guesses against one account. The per-address limit alone would not stop
     * someone spreading attempts across many addresses to attack a single account.
     */
    public void requireAttemptAllowance(String email) {
        var config = properties.getRateLimit();
        if (!rateLimiter.tryAcquire("attempt:" + email, config.getPerEmailLoginAttempts(),
                Duration.ofMinutes(config.getPerEmailLoginWindowMinutes()))) {
            throw new TooManyRequestsException("Demasiados intentos. Esperá unos minutos.");
        }
    }
}
