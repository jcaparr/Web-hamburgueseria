package com.hamburguesas.service;

import com.hamburguesas.auth.GoogleTokenVerifier;
import com.hamburguesas.dto.AuthResponse;
import com.hamburguesas.dto.GoogleLoginRequest;
import com.hamburguesas.exception.WrongSignInMethodException;
import com.hamburguesas.model.User;
import com.hamburguesas.repository.UserRepository;
import com.hamburguesas.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Signing in with Google.
 *
 * Each account belongs to exactly one sign-in method. An address that already has a
 * password account here is not a Google account, and is told so rather than merged:
 * one way in per account is far easier to reason about, and to audit, than accounts
 * that can be reached from two directions.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GoogleAuthService {

    private final GoogleTokenVerifier tokenVerifier;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse login(GoogleLoginRequest request) {
        GoogleTokenVerifier.GoogleAccount account = verifyOrReject(request.credential());

        Optional<User> byGoogle = userRepository.findByGoogleSub(account.subject());
        if (byGoogle.isPresent()) {
            return sessionFor(byGoogle.get());
        }

        Optional<User> byEmail = userRepository.findByEmail(account.email());
        if (byEmail.isPresent()) {
            // Saying so plainly leaks nothing: getting here means Google already
            // confirmed the caller owns this address, so they are the account's owner.
            if (byEmail.get().getGoogleSub() != null) {
                log.warn("Google sign-in for an email already tied to a different Google account");
                throw new BadCredentialsException("No pudimos iniciar sesión con Google");
            }
            throw new WrongSignInMethodException(
                "Ese email ya tiene una cuenta con contraseña. Entrá con tu contraseña.");
        }

        // Google has already verified the address, so the account works straight away
        // and never has a password.
        User user = userRepository.save(User.builder()
            .name(account.name())
            .email(account.email())
            .googleSub(account.subject())
            .emailVerified(true)
            .build());

        return sessionFor(user);
    }

    private GoogleTokenVerifier.GoogleAccount verifyOrReject(String credential) {
        if (!tokenVerifier.isConfigured()) {
            // The caller only gets the usual generic refusal, so say it plainly here:
            // otherwise a missing GOOGLE_CLIENT_ID looks exactly like a bad token.
            log.warn("Google sign-in was attempted but GOOGLE_CLIENT_ID is not configured");
            throw new BadCredentialsException("El ingreso con Google no está disponible");
        }
        return tokenVerifier.verify(credential)
            .orElseThrow(() -> new BadCredentialsException("No pudimos iniciar sesión con Google"));
    }

    private AuthResponse sessionFor(User user) {
        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(token, user.getId(), user.getName(), user.getEmail());
    }
}
