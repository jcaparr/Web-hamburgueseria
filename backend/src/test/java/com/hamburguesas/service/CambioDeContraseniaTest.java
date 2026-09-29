package com.hamburguesas.service;

import com.hamburguesas.auth.AuthRateLimits;
import com.hamburguesas.auth.SessionRevoker;
import com.hamburguesas.auth.VerificationService;
import com.hamburguesas.dto.ResetPasswordRequest;
import com.hamburguesas.exception.InvalidCodeException;
import com.hamburguesas.model.User;
import com.hamburguesas.model.VerificationPurpose;
import com.hamburguesas.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cubre que cambiar la contraseña cierre las sesiones abiertas.
 *
 * Es la maniobra de quien sospecha que le entraron a la cuenta, y hasta ahora no echaba
 * a nadie: el refresh token dura treinta días y se renueva solo, así que una sesión
 * robada seguía viva indefinidamente después del cambio. El método para cortarlas
 * existía y no lo llamaba nadie.
 */
class CambioDeContraseniaTest {

    private UserRepository userRepository;
    private SessionRevoker sessionRevoker;
    private VerificationService verificationService;
    private AuthService service;

    private User usuario;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        sessionRevoker = mock(SessionRevoker.class);
        verificationService = mock(VerificationService.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

        service = new AuthService(
            userRepository, sessionRevoker, passwordEncoder,
            mock(AuthenticationManager.class), verificationService,
            mock(AuthRateLimits.class),
            mock(UsernameService.class));

        usuario = User.builder()
            .id(7L).username("juan").email("juan@example.com")
            .passwordHash("la-vieja").emailVerified(true)
            .build();

        when(userRepository.findByEmail("juan@example.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.encode(anyString())).thenReturn("la-nueva");
        when(verificationService.check(any(), any(), anyString()))
            .thenReturn(VerificationService.Result.OK);
    }

    private ResetPasswordRequest pedido() {
        return new ResetPasswordRequest("juan@example.com", "123456", "una-contrasenia-larga");
    }

    @Test
    void cambiarLaContraseniaCierraLasSesionesAbiertas() {
        service.resetPassword(pedido());

        verify(sessionRevoker).revokeAllFor(usuario);
    }

    @Test
    void yGuardaLaContraseniaNueva() {
        service.resetPassword(pedido());

        assertThat(usuario.getPasswordHash()).isEqualTo("la-nueva");
        verify(userRepository).save(usuario);
    }

    /**
     * Con un código que no sirve no se cambia nada, así que tampoco se echa a nadie:
     * si no, cualquiera podría cerrarle la sesión a otro tirando códigos al azar.
     */
    @Test
    void conUnCodigoInvalidoNoSeCierraNingunaSesion() {
        when(verificationService.check(any(), any(), anyString()))
            .thenReturn(VerificationService.Result.INVALID);

        assertThatThrownBy(() -> service.resetPassword(pedido()))
            .isInstanceOf(InvalidCodeException.class);

        verify(sessionRevoker, never()).revokeAllFor(any());
        verify(userRepository, never()).save(any());
    }

    /** Una cuenta de Google no tiene contraseña que cambiar, y no se le inventa una. */
    @Test
    void unaCuentaDeGoogleNoSePuedeCambiarPorAca() {
        usuario.setGoogleSub("google-123");

        assertThatThrownBy(() -> service.resetPassword(pedido()))
            .isInstanceOf(InvalidCodeException.class);

        verify(sessionRevoker, never()).revokeAllFor(any());
    }

    /**
     * El código de recuperación se pide por email, así que una cuenta que nunca lo
     * confirmó no puede probar que es suya.
     */
    @Test
    void unaCuentaSinVerificarNoSePuedeCambiar() {
        usuario.setEmailVerified(false);

        assertThatThrownBy(() -> service.resetPassword(pedido()))
            .isInstanceOf(InvalidCodeException.class);

        verify(sessionRevoker, never()).revokeAllFor(any());
    }

    /** Y el código se valida contra el propósito de recuperar, no contra cualquiera. */
    @Test
    void validaElCodigoDeRecuperarContrasenia() {
        service.resetPassword(pedido());

        verify(verificationService).check(usuario, VerificationPurpose.PASSWORD_RESET, "123456");
    }
}
