package com.hamburguesas.service;

import com.hamburguesas.auth.AuthRateLimits;
import com.hamburguesas.auth.PwnedPasswordChecker;
import com.hamburguesas.auth.SessionRevoker;
import com.hamburguesas.auth.VerificationService;
import com.hamburguesas.dto.EmailOnlyRequest;
import com.hamburguesas.dto.LoginRequest;
import com.hamburguesas.dto.MessageResponse;
import com.hamburguesas.dto.RegisterRequest;
import com.hamburguesas.dto.ResetPasswordRequest;
import com.hamburguesas.dto.VerifyEmailRequest;
import com.hamburguesas.exception.ConflictException;
import com.hamburguesas.exception.EmailNotVerifiedException;
import com.hamburguesas.exception.InvalidCodeException;
import com.hamburguesas.exception.WeakPasswordException;
import com.hamburguesas.model.User;
import com.hamburguesas.model.VerificationPurpose;
import com.hamburguesas.repository.UserRepository;
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
 * Registration is the one place that admits an address already has an account, so the
 * person is not left waiting for a code that is never coming. Everywhere else the
 * answer is the same whether or not the account exists: those flows gain nothing from
 * saying so, and each one that does is another way to find out who is registered.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    /** For the flows that must not admit whether the address has an account. */
    private static final String CHECK_YOUR_EMAIL =
        "Si el email es válido, te mandamos un código. Revisá también la carpeta de spam.";

    /** Registration can be direct: it already refuses an address that is taken. */
    private static final String CODE_SENT =
        "Te mandamos un código a tu email. Revisá también la carpeta de spam.";

    private static final String CODE_REJECTED = "El código no es válido o venció";

    private final UserRepository userRepository;
    private final SessionRevoker sessionRevoker;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final VerificationService verificationService;
    private final AuthRateLimits rateLimits;
    private final PwnedPasswordChecker pwnedPasswordChecker;

    @Transactional
    public MessageResponse register(RegisterRequest request) {
        String email = normalize(request.email());
        requireSendAllowance(email);
        Optional<User> existing = userRepository.findByEmail(email);

        if (existing.isPresent()) {
            User user = existing.get();

            // Still unverified: almost certainly the same person retrying, so send
            // another code instead of telling them they are in their own way.
            if (!user.isEmailVerified()) {
                issueQuietly(user, VerificationPurpose.EMAIL_VERIFICATION);
                return new MessageResponse(CODE_SENT);
            }

            // Saying the address is taken is a deliberate trade: it lets anyone test
            // addresses to learn who has an account here. It is accepted because the
            // alternative left people staring at a code screen waiting for a code that
            // was never coming. The other flows that take an email stay generic, so
            // this is the only place that admits an account exists.
            //
            // Which method the account uses is not named, and costs the user nothing:
            // the login screen offers both, so they will find theirs there either way.
            throw new ConflictException("Ese email ya tiene una cuenta. Probá iniciar sesión.");
        }

        requireUnbreachedPassword(request.password());

        User user = userRepository.save(User.builder()
            .name(request.name().trim())
            .email(email)
            .passwordHash(passwordEncoder.encode(request.password()))
            .emailVerified(false)
            .build());

        // Inside the transaction on purpose: if the email cannot be sent, the account
        // is rolled back rather than left stranded with no way to activate it.
        verificationService.issue(user, VerificationPurpose.EMAIL_VERIFICATION);
        return new MessageResponse(CODE_SENT);
    }

    /** @return the activated user, for the caller to turn into a session. */
    @Transactional
    public User verifyEmail(VerifyEmailRequest request) {
        requireAttemptAllowance(normalize(request.email()));
        User user = userRepository.findByEmail(normalize(request.email()))
            .filter(candidate -> !candidate.isEmailVerified())
            .orElseThrow(() -> new InvalidCodeException(CODE_REJECTED));

        requireValidCode(user, VerificationPurpose.EMAIL_VERIFICATION, request.code());

        user.setEmailVerified(true);
        return userRepository.save(user);
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
            // A Google account has no password to recover, and handing it one would be
            // a way in that the account was never meant to have.
            .filter(user -> user.getGoogleSub() == null)
            .ifPresent(user -> issueQuietly(user, VerificationPurpose.PASSWORD_RESET));
        return new MessageResponse(CHECK_YOUR_EMAIL);
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        requireAttemptAllowance(normalize(request.email()));
        User user = userRepository.findByEmail(normalize(request.email()))
            .filter(User::isEmailVerified)
            .filter(candidate -> candidate.getGoogleSub() == null)
            .orElseThrow(() -> new InvalidCodeException(CODE_REJECTED));

        // Antes de validar el código, no después: validarlo lo consume, y rechazar
        // después la contraseña obligaría a pedir un código nuevo para reintentar.
        requireUnbreachedPassword(request.newPassword());

        requireValidCode(user, VerificationPurpose.PASSWORD_RESET, request.code());

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        // Cambiar la contraseña es lo que hace alguien que sospecha que le entraron, así
        // que tiene que echar al que entró. Sin esto no echaba a nadie: el refresh token
        // dura treinta días y se renueva solo, así que una sesión robada sobrevivía a la
        // única maniobra que existe para recuperar la cuenta.
        int cerradas = sessionRevoker.revokeAllFor(user);
        if (cerradas > 0) {
            log.info("Se cerraron {} sesiones al cambiar la contraseña del usuario {}",
                cerradas, user.getId());
        }

        // No token here: whoever reset the password still has to log in with it, so a
        // stolen code on its own does not hand over a live session.
        return new MessageResponse("Listo, ya podés entrar con tu nueva contraseña.");
    }

    public User login(LoginRequest request) {
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

        return user;
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

    private void requireSendAllowance(String email) {
        rateLimits.requireSendAllowance(email);
    }

    private void requireAttemptAllowance(String email) {
        rateLimits.requireAttemptAllowance(email);
    }

    /**
     * No mide fuerza sino reuso: una contraseña larga y con símbolos no sirve de nada
     * si ya está en una lista publicada, porque es lo primero que se prueba.
     */
    private void requireUnbreachedPassword(String password) {
        if (pwnedPasswordChecker.isBreached(password)) {
            throw new WeakPasswordException(
                "Esa contraseña apareció en filtraciones conocidas. Elegí otra.");
        }
    }

    /** Emails are case-insensitive in practice, and the column is unique. */
    private String normalize(String email) {
        return email.trim().toLowerCase();
    }
}
