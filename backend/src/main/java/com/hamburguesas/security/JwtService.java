package com.hamburguesas.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMinutes;

    public JwtService(
        @Value("${app.jwt.secret}") String secret,
        // La misma duración que la cookie que lo lleva. Antes era aparte, de 60 minutos
        // contra 15 de la cookie: un token robado servía cuatro veces más de lo pensado,
        // y un JWT no se puede revocar.
        @Value("${app.auth.session.access-token-minutes}") long expirationMinutes
    ) {
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            // Fail at boot with a readable message instead of letting HS256 be signed
            // with a key short enough to brute force.
            throw new IllegalStateException(
                "app.jwt.secret must be at least 32 characters (got " + secretBytes.length + "). "
                    + "Set the JWT_SECRET environment variable to a long random value.");
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.expirationMinutes = expirationMinutes;
    }

    public String generateToken(Long userId, String email) {
        Instant now = Instant.now();
        return Jwts.builder()
            .subject(String.valueOf(userId))
            .claim("email", email)
            .issuedAt(java.util.Date.from(now))
            .expiration(java.util.Date.from(now.plus(expirationMinutes, ChronoUnit.MINUTES)))
            .signWith(key)
            .compact();
    }

    public Long extractUserId(String token) {
        String subject = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload()
            .getSubject();
        return Long.valueOf(subject);
    }
}
