package com.hamburguesas.service;

import com.hamburguesas.auth.AuthRateLimits;
import com.hamburguesas.auth.VerificationService;
import com.hamburguesas.dto.AuthResponse;
import com.hamburguesas.dto.EmailOnlyRequest;
import com.hamburguesas.dto.LoginRequest;
import com.hamburguesas.dto.MessageResponse;
import com.hamburguesas.dto.RegisterRequest;
import com.hamburguesas.dto.ResetPasswordRequest;
import com.hamburguesas.dto.VerifyEmailRequest;
import com.hamburguesas.exception.EmailNotVerifiedException;
import com.hamburguesas.exception.InvalidCodeException;
import com.hamburguesas.mail.EmailService;
import com.hamburguesas.model.User;
import com.hamburguesas.model.VerificationPurpose;
import com.hamburguesas.repository.UserRepository;
import com.hamburguesas.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Registration, email verification and password recovery.
 *
 * Every entry point that takes an email answers the same thing whether or not that
 * address has an account. Otherwise this API would be a way to find out who is
 * registered, which is both a privacy leak and a list worth spamming.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private static final String CHECK_YOUR_EMAIL =
        "Si el email es válido, te mandamos un código. Revisá también la carpeta de spam.";

    private static final String CODE_REJECTED = "El código no es válido o venció";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final VerificationService verificationService;
    private final EmailService emailService;
    private final AuthRateLimits rateLimits;

    /**
     * Never reports that the address is taken. If it is, the owner gets an email
     * about it instead: they are the only person entitled to know.
     */
    @Transactional
    public MessageResponse register(RegisterRequest request) {
        String email = normalize(request.email());
        requireSendAllowance(email);
        Optional<User> existing = userRepository.findByEmail(email);

        if (existing.isPresent()) {
            User user = existing.get();
            if (user.isEmailVerified()) {
                notifyRegistrationAttempt(user);
            } else {
                // Same address, still unverified: most likely the same person retrying.
                issueQuietly(user, VerificationPurpose.EMAIL_VERIFICATION);
            }
            return new MessageResponse(CHECK_YOUR_EMAIL);
        }

        User user = userRepository.save(User.builder()
            .name(request.name().trim())
            .email(email)
            .passwordHash(passwordEncoder.encode(request.password()))
            .emailVerified(false)
            .build());

        // Inside the transaction on purpose: if the email cannot be sent, the account
        // is rolled back rather than left stranded with no way to activate it.
        verificationService.issue(user, VerificationPurpose.EMAIL_VERIFICATION);
        return new MessageResponse(CHECK_YOUR_EMAIL);
    }

    @Transactional
    public AuthResponse verifyEmail(VerifyEmailRequest request) {
        requireAttemptAllowance(normalize(request.email()));
        User user = userRepository.findByEmail(normalize(request.email()))
            .filter(candidate -> !candidate.isEmailVerified())
            .orElseThrow(() -> new InvalidCodeException(CODE_REJECTED));

        requireValidCode(user, VerificationPurpose.EMAIL_VERIFICATION, request.code());

        user.setEmailVerified(true);
        userRepository.save(user);

        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(token, user.getId(), user.getName(), user.getEmail());
    }

    @Transactional
    public MessageResponse resendVerificationCode(EmailOnlyRequest request) {
        requireSendAllowance(normalize(request.email()));
        userRepository.findByEmail(normalize(request.email()))
            .filter(user -> !user.isEmailVerified())
            .ifPresent(user -> issueQuietly(user, VerificationPurpose.EMAIL_VERIFICATION));
        return new MessageResponse(CHECK_YOUR_EMAIL);
    }

    @Transactional
    public MessageResponse forgotPassword(EmailOnlyRequest request) {
        requireSendAllowance(normalize(request.email()));
        userRepository.findByEmail(normalize(request.email()))
            // An unverified account has never proved it owns the address, so sending it
            // a reset code would hand the account to whoever typed that address.
            .filter(User::isEmailVerified)
            .ifPresent(user -> issueQuietly(user, VerificationPurpose.PASSWORD_RESET));
        return new MessageResponse(CHECK_YOUR_EMAIL);
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        requireAttemptAllowance(normalize(request.email()));
        User user = userRepository.findByEmail(normalize(request.email()))
            .filter(User::isEmailVerified)
            .orElseThrow(() -> new InvalidCodeException(CODE_REJECTED));

        requireValidCode(user, VerificationPurpose.PASSWORD_RESET, request.code());

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        // No token here: whoever reset the password still has to log in with it, so a
        // stolen code on its own does not hand over a live session.
        return new MessageResponse("Listo, ya podés entrar con tu nueva contraseña.");
    }

    public AuthResponse login(LoginRequest request) {
        String email = normalize(request.email());
        requireAttemptAllowance(email);

        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(email, request.password())
        );

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalStateException("User not found after authentication"));

        // Checked here rather than through Spring's "disabled account" flag, which is
        // evaluated before the password and would leak which emails are registered.
        // Getting this far means the caller already knows the password.
        if (!user.isEmailVerified()) {
            issueQuietly(user, VerificationPurpose.EMAIL_VERIFICATION);
            throw new EmailNotVerifiedException(
                "Te falta activar la cuenta. Te mandamos un código nuevo por email.");
        }

        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(token, user.getId(), user.getName(), user.getEmail());
    }

    private void requireValidCode(User user, VerificationPurpose purpose, String code) {
        switch (verificationService.check(user, purpose, code)) {
            case OK -> { }
            case EXPIRED -> throw new InvalidCodeException("El código venció. Pedí uno nuevo.");
            case TOO_MANY_ATTEMPTS -> throw new InvalidCodeException(
                "Demasiados intentos con este código. Pedí uno nuevo.");
            case INVALID -> throw new InvalidCodeException(CODE_REJECTED);
        }
    }

    /**
     * For the flows that answer the same either way: a delivery failure must not turn
     * into an error that reveals whether the address exists.
     */
    private void issueQuietly(User user, VerificationPurpose purpose) {
        try {
            verificationService.issue(user, purpose);
        } catch (RuntimeException ex) {
            log.error("Could not issue a {} code: {}", purpose, ex.getMessage());
        }
    }

    private void notifyRegistrationAttempt(User user) {
        String body = """
            ¡Hola, %s!

            Alguien intentó crear una cuenta con tu email. Como ya tenés una, no creamos
            ninguna cuenta nueva ni cambiamos nada.

            Si fuiste vos, entrá con tu contraseña de siempre. Si no la recordás, usá la
            opción "Olvidé mi contraseña".

            Si no fuiste vos, podés ignorar este mail tranquilo.

            Hamburgueserías BA
            """.formatted(user.getName());

        try {
            emailService.send(user.getEmail(), "Ya tenés una cuenta con este email", body);
        } catch (RuntimeException ex) {
            log.error("Could not send the duplicate registration notice: {}", ex.getMessage());
        }
    }

    private void requireSendAllowance(String email) {
        rateLimits.requireSendAllowance(email);
    }

    private void requireAttemptAllowance(String email) {
        rateLimits.requireAttemptAllowance(email);
    }

    /** Emails are case-insensitive in practice, and the column is unique. */
    private String normalize(String email) {
        return email.trim().toLowerCase();
    }
}
