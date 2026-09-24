package com.hamburguesas.auth;

import com.hamburguesas.model.RefreshToken;
import com.hamburguesas.model.User;
import com.hamburguesas.repository.RefreshTokenRepository;
import com.hamburguesas.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;

/**
 * Issues and rotates the refresh half of a session.
 *
 * Every use of a refresh token replaces it. That way a stolen token is only good
 * until the real user refreshes once, and the theft becomes visible: the loser of
 * the race presents a token that has already been used.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SessionService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final SessionRevoker sessionRevoker;
    private final UserRepository userRepository;
    private final AuthProperties properties;
    private final SecureRandom random = new SecureRandom();

    /** The raw token, which is handed to the browser and never stored. */
    public record IssuedToken(String value, Instant expiresAt) {}

    @Transactional
    public IssuedToken issue(User user) {
        // 256 bits, so guessing is not a threat model and the hash below does not
        // need to be slow.
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String value = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        Instant expiresAt = Instant.now()
            .plus(Duration.ofDays(properties.getSession().getRefreshTokenDays()));

        refreshTokenRepository.save(RefreshToken.builder()
            .user(user)
            .tokenHash(hash(value))
            .expiresAt(expiresAt)
            .build());

        return new IssuedToken(value, expiresAt);
    }

    /**
     * Swaps a valid refresh token for a fresh one.
     *
     * @throws SessionRejectedException when the token is unknown, expired, or already
     *         used. A used token means someone has a copy they should not, so the
     *         whole session family goes: there is no way to tell which of the two is
     *         the real user, and logging both out is the safe answer.
     */
    @Transactional
    public RotatedSession rotate(String presentedToken) {
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash(presentedToken))
            .orElseThrow(() -> new SessionRejectedException("La sesión venció"));

        // Loaded through the repository rather than stored.getUser(), which hands back
        // a lazy proxy: the caller reads the name and email to build the response,
        // by which point this transaction is closed and the proxy throws.
        User user = userRepository.findById(stored.getUser().getId())
            .orElseThrow(() -> new SessionRejectedException("La sesión venció"));

        if (stored.getRevokedAt() != null) {
            log.warn("A refresh token was presented after being revoked; cutting every "
                + "session for user {}", user.getId());
            // In its own transaction: the throw below rolls this one back, and would
            // undo the revocation it is reporting.
            sessionRevoker.revokeAllFor(user);
            throw new SessionRejectedException("La sesión venció");
        }

        if (!Instant.now().isBefore(stored.getExpiresAt())) {
            throw new SessionRejectedException("La sesión venció");
        }

        stored.setRevokedAt(Instant.now());
        refreshTokenRepository.save(stored);

        return new RotatedSession(user, issue(user));
    }

    public record RotatedSession(User user, IssuedToken refreshToken) {}

    /** Ends one session. Other devices keep theirs. */
    @Transactional
    public void revoke(String presentedToken) {
        refreshTokenRepository.findByTokenHash(hash(presentedToken)).ifPresent(token -> {
            token.setRevokedAt(Instant.now());
            refreshTokenRepository.save(token);
        });
    }

    /**
     * Expired rows cannot authorise anything, so they are only a growing table and a
     * pile of hashes worth stealing.
     */
    @Scheduled(fixedDelay = 6, timeUnit = TimeUnit.HOURS)
    @Transactional
    public void deleteExpiredTokens() {
        int deleted = refreshTokenRepository.deleteExpiredBefore(Instant.now());
        if (deleted > 0) {
            log.info("Deleted {} expired refresh tokens", deleted);
        }
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            // Every JVM ships SHA-256; if this happens the platform is broken.
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }
}
