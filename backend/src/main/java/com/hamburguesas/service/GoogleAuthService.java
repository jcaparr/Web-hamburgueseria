package com.hamburguesas.service;

import com.hamburguesas.auth.AuthRateLimits;
import com.hamburguesas.auth.GoogleTokenVerifier;
import com.hamburguesas.auth.VerificationService;
import com.hamburguesas.dto.AuthResponse;
import com.hamburguesas.dto.GoogleLinkRequest;
import com.hamburguesas.dto.GoogleLoginRequest;
import com.hamburguesas.exception.GoogleLinkRequiredException;
import com.hamburguesas.exception.InvalidCodeException;
import com.hamburguesas.model.User;
import com.hamburguesas.model.VerificationPurpose;
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
 * Three cases, and the third is the one that matters: an address that already has an
 * account here. Accepting the Google token on its own would mean anyone who can get
 * Google to assert an address takes over the account using it. So that case is not a
 * login, it is a request to link, and it is confirmed by email first.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GoogleAuthService {

    private static final String CODE_REJECTED = "El código no es válido o venció";

    private final GoogleTokenVerifier tokenVerifier;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final VerificationService verificationService;
    private final AuthRateLimits rateLimits;

    @Transactional
    public AuthResponse login(GoogleLoginRequest request) {
        GoogleTokenVerifier.GoogleAccount account = verifyOrReject(request.credential());

        Optional<User> byGoogle = userRepository.findByGoogleSub(account.subject());
        if (byGoogle.isPresent()) {
            return sessionFor(byGoogle.get());
        }

        Optional<User> byEmail = userRepository.findByEmail(account.email());
        if (byEmail.isPresent()) {
            throw linkRequired(byEmail.get());
        }

        // Nobody here yet: Google has already verified the address, so the account works
        // straight away and never needs a password.
        User user = userRepository.save(User.builder()
            .name(account.name())
            .email(account.email())
            .googleSub(account.subject())
            .emailVerified(true)
            .build());

        return sessionFor(user);
    }

    /** Completes the link once the emailed code is entered. */
    @Transactional
    public AuthResponse confirmLink(GoogleLinkRequest request) {
        GoogleTokenVerifier.GoogleAccount account = verifyOrReject(request.credential());

        rateLimits.requireAttemptAllowance(account.email());

        User user = userRepository.findByEmail(account.email())
            .filter(candidate -> candidate.getGoogleSub() == null)
            .orElseThrow(() -> new InvalidCodeException(CODE_REJECTED));

        switch (verificationService.check(user, VerificationPurpose.GOOGLE_LINK, request.code())) {
            case OK -> { }
            case EXPIRED -> throw new InvalidCodeException("El código venció. Pedí uno nuevo.");
            case TOO_MANY_ATTEMPTS -> throw new InvalidCodeException(
                "Demasiados intentos con este código. Pedí uno nuevo.");
            case INVALID -> throw new InvalidCodeException(CODE_REJECTED);
        }

        user.setGoogleSub(account.subject());
        // Google verified the address and the code proved it again, so an account that
        // was still sitting unactivated is activated by this.
        user.setEmailVerified(true);
        userRepository.save(user);

        return sessionFor(user);
    }

    /**
     * @return the exception to throw. Sends the confirmation code on the way, unless
     *         the address is already linked to a different Google account, which is
     *         refused outright: whatever that is, it is not the owner coming back.
     */
    private RuntimeException linkRequired(User user) {
        if (user.getGoogleSub() != null) {
            log.warn("Google sign-in for an email already linked to a different Google account");
            return new BadCredentialsException("No pudimos iniciar sesión con Google");
        }

        rateLimits.requireSendAllowance(user.getEmail());
        try {
            verificationService.issue(user, VerificationPurpose.GOOGLE_LINK);
        } catch (RuntimeException ex) {
            log.error("Could not issue a Google link code: {}", ex.getMessage());
        }

        return new GoogleLinkRequiredException(
            "Ya tenés una cuenta con ese email. Te mandamos un código para vincularla "
                + "con Google. Revisá también la carpeta de spam.");
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
