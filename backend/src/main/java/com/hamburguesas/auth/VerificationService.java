package com.hamburguesas.auth;

import com.hamburguesas.mail.EmailService;
import com.hamburguesas.model.User;
import com.hamburguesas.model.VerificationCode;
import com.hamburguesas.model.VerificationPurpose;
import com.hamburguesas.repository.VerificationCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Issues and checks the codes emailed to users.
 *
 * The rules that make a 6-digit code safe live here: it expires, it can only be
 * guessed a handful of times, asking for a new one invalidates the old, and it is
 * stored hashed.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class VerificationService {

    private final VerificationCodeRepository codeRepository;
    private final VerificationAttemptRecorder attemptRecorder;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuthProperties properties;
    private final SecureRandom random = new SecureRandom();

    /** Result of checking a code, so callers can react without catching exceptions. */
    public enum Result {
        OK,
        INVALID,
        EXPIRED,
        TOO_MANY_ATTEMPTS
    }

    /**
     * @return false when the user asked for another code too soon, in which case
     *         nothing is sent. Callers still answer the same either way.
     */
    @Transactional
    public boolean issue(User user, VerificationPurpose purpose) {
        var config = properties.getVerification();
        Instant now = Instant.now();

        Optional<VerificationCode> latest =
            codeRepository.findFirstByUserAndPurposeOrderByCreatedAtDesc(user, purpose);

        if (latest.isPresent()) {
            Instant nextAllowed = latest.get().getCreatedAt()
                .plusSeconds(config.getResendCooldownSeconds());
            if (now.isBefore(nextAllowed)) {
                return false;
            }
        }

        // An old code left sitting in an inbox must stop working once a new one exists.
        codeRepository.consumeAllFor(user, purpose);

        String code = generateCode(config.getCodeLength());
        codeRepository.save(VerificationCode.builder()
            .user(user)
            .purpose(purpose)
            .codeHash(passwordEncoder.encode(code))
            .expiresAt(now.plus(Duration.ofMinutes(config.getTtlMinutes())))
            .attempts(0)
            .createdAt(now)
            .build());

        emailService.send(user.getEmail(), subjectFor(purpose),
            bodyFor(purpose, user.getName(), code, config.getTtlMinutes()));
        return true;
    }

    /**
     * Same as {@link #issue}, but in a transaction of its own.
     *
     * For callers that email a code and then throw, to tell the frontend the flow is
     * not finished. Sharing their transaction would roll the code back while the email
     * has already gone out, leaving the user holding a code that was never saved.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean issueSeparately(User user, VerificationPurpose purpose) {
        return issue(user, purpose);
    }

    @Transactional
    public Result check(User user, VerificationPurpose purpose, String code) {
        var config = properties.getVerification();
        Instant now = Instant.now();

        VerificationCode stored = codeRepository
            .findFirstByUserAndPurposeOrderByCreatedAtDesc(user, purpose)
            .orElse(null);

        if (stored == null || stored.getConsumedAt() != null) {
            return Result.INVALID;
        }
        if (now.isAfter(stored.getExpiresAt())) {
            return Result.EXPIRED;
        }
        if (stored.getAttempts() >= config.getMaxAttempts()) {
            return Result.TOO_MANY_ATTEMPTS;
        }

        // In its own transaction: the caller throws on a bad code, and that rollback
        // would otherwise undo the count and leave the attempt limit doing nothing.
        stored.setAttempts(attemptRecorder.record(stored.getId()));

        if (!passwordEncoder.matches(code, stored.getCodeHash())) {
            return Result.INVALID;
        }

        stored.setConsumedAt(now);
        codeRepository.save(stored);
        return Result.OK;
    }

    private String generateCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    // Switches rather than ternaries, so adding a purpose without writing its wording
    // fails to compile instead of silently sending the wrong email.
    private String subjectFor(VerificationPurpose purpose) {
        return switch (purpose) {
            case EMAIL_VERIFICATION -> "Tu código para activar la cuenta";
            case PASSWORD_RESET -> "Tu código para cambiar la contraseña";
            case GOOGLE_LINK -> "Tu código para vincular tu cuenta con Google";
        };
    }

    private String bodyFor(VerificationPurpose purpose, String name, String code, int ttlMinutes) {
        String action = switch (purpose) {
            case EMAIL_VERIFICATION -> "activar tu cuenta";
            case PASSWORD_RESET -> "cambiar tu contraseña";
            case GOOGLE_LINK -> "vincular tu cuenta con Google";
        };

        // Plain text on purpose: an HTML email from a brand-new sender is likelier
        // to be filtered as spam, and we have no domain to authenticate with yet.
        return """
            ¡Hola, %s!

            Tu código para %s es:

                %s

            Vence en %d minutos. Si no fuiste vos, ignorá este mail: sin el código no
            pasa nada.

            ¿No lo ves? Revisá la carpeta de spam o correo no deseado.

            Hamburgueserías BA
            """.formatted(name, action, code, ttlMinutes);
    }
}
