package com.hamburguesas.auth;

import com.hamburguesas.dto.VerifyEmailRequest;
import com.hamburguesas.exception.InvalidCodeException;
import com.hamburguesas.model.User;
import com.hamburguesas.model.VerificationCode;
import com.hamburguesas.model.VerificationPurpose;
import com.hamburguesas.repository.RefreshTokenRepository;
import com.hamburguesas.repository.UserRepository;
import com.hamburguesas.repository.VerificationCodeRepository;
import com.hamburguesas.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Two defences that were silently doing nothing, and the shape of mistake that broke
 * both: a transactional method that writes something to the database and then throws
 * to report the problem. The throw rolls back the very record the throw is about.
 *
 * Both were found by hand, against a real database. These tests exist so the next
 * one is found by the build instead.
 */
@SpringBootTest
@ActiveProfiles("test")
class TransactionBoundaryTest {

    @Autowired private AuthService authService;
    @Autowired private SessionService sessionService;
    @Autowired private VerificationService verificationService;
    @Autowired private UserRepository userRepository;
    @Autowired private VerificationCodeRepository codeRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;

    private User user;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        codeRepository.deleteAll();
        userRepository.deleteAll();

        user = userRepository.save(User.builder()
            .name("Probador")
            .username("probador")
            .email("probador@example.com")
            .passwordHash("no-se-usa-en-este-test")
            .emailVerified(false)
            .build());
    }

    @Test
    @DisplayName("un código equivocado gasta un intento, aunque el rechazo revierta la transacción")
    void failedAttemptSurvivesTheRejection() {
        verificationService.issue(user, VerificationPurpose.EMAIL_VERIFICATION);

        assertThatThrownBy(() -> authService.verifyEmail(
            new VerifyEmailRequest(user.getEmail(), "000000")))
            .isInstanceOf(InvalidCodeException.class);

        VerificationCode code = codeRepository
            .findFirstByUserAndPurposeOrderByCreatedAtDesc(user, VerificationPurpose.EMAIL_VERIFICATION)
            .orElseThrow();

        // Con el incremento dentro de la transacción del llamador, esto quedaba en 0
        // para siempre: el límite de 5 intentos no existía y un código de 6 dígitos
        // quedaba expuesto a fuerza bruta.
        assertThat(code.getAttempts()).isEqualTo(1);
    }

    @Test
    @DisplayName("reusar un refresh token corta todas las sesiones, aunque el rechazo revierta")
    void reusingARefreshTokenCutsEverySession() {
        SessionService.IssuedToken deUnDispositivo = sessionService.issue(user);
        SessionService.IssuedToken deOtroDispositivo = sessionService.issue(user);

        // Usar una vez es lo normal: queda revocada y aparece su reemplazo.
        sessionService.rotate(deUnDispositivo.value());

        assertThatThrownBy(() -> sessionService.rotate(deUnDispositivo.value()))
            .isInstanceOf(SessionRejectedException.class);

        // Presentar un token ya usado es la señal de que alguien tiene una copia. Con
        // la revocación dentro de la transacción que el throw revierte, la sesión
        // robada seguía viva y la defensa era decorativa.
        assertThat(refreshTokenRepository.findAll())
            .isNotEmpty()
            .allSatisfy(token -> assertThat(token.getRevokedAt()).isNotNull());

        // Incluida la del otro dispositivo, que nadie tocó: no hay forma de saber cuál
        // de los dos es el dueño, así que se van las dos.
        assertThatThrownBy(() -> sessionService.rotate(deOtroDispositivo.value()))
            .isInstanceOf(SessionRejectedException.class);
    }
}
