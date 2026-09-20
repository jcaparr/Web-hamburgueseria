package com.hamburguesas.auth;

import com.hamburguesas.model.User;
import com.hamburguesas.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cuts every session of one user, in a transaction of its own.
 *
 * It has to survive the caller's rollback. A reused refresh token is reported by
 * throwing, and that rollback would undo the very revocation the throw is about,
 * leaving the stolen session alive. Same reason VerificationAttemptRecorder exists.
 */
@Component
@RequiredArgsConstructor
public class SessionRevoker {

    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int revokeAllFor(User user) {
        return refreshTokenRepository.revokeAllFor(user, Instant.now());
    }
}
