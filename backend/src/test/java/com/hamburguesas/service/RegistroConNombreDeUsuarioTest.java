package com.hamburguesas.service;

import com.hamburguesas.auth.AuthRateLimits;
import com.hamburguesas.auth.SessionRevoker;
import com.hamburguesas.auth.VerificationService;
import com.hamburguesas.dto.RegisterRequest;
import com.hamburguesas.exception.UsernameTakenException;
import com.hamburguesas.model.User;
import com.hamburguesas.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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

/** El nombre de usuario en el registro con contraseña. */
class RegistroConNombreDeUsuarioTest {

    private UserRepository userRepository;
    private AuthService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(llamada -> llamada.getArgument(0));
        when(passwordEncoder.encode(anyString())).thenReturn("el-hash");

        service = new AuthService(
            userRepository, mock(SessionRevoker.class), passwordEncoder,
            mock(AuthenticationManager.class), mock(VerificationService.class),
            mock(AuthRateLimits.class),
            new UsernameService(userRepository));
    }

    private RegisterRequest pidiendo(String username) {
        return new RegisterRequest(username, "juan@example.com", "unaClaveLarga123");
    }

    /** Lo guardado es siempre en minúsculas, escriba como escriba. */
    private User loGuardado() {
        ArgumentCaptor<User> guardado = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(guardado.capture());
        return guardado.getValue();
    }

    @Test
    void laCuentaNuevaQuedaConSuNombreEnMinusculas() {
        service.register(pidiendo("JuanCa"));

        assertThat(loGuardado().getUsername()).isEqualTo("juanca");
    }

    /**
     * Cualquier contraseña sirve, incluso una de las que están en todos los dumps.
     *
     * Hubo un chequeo contra Have I Been Pwned que rechazaba las filtradas, y se sacó a
     * pedido: trababa a gente que no entendía por qué. Lo que se acepta a cambio es el
     * relleno de credenciales —probar acá una contraseña sacada de otra filtración—,
     * contra lo que siguen estando el límite de intentos por cuenta y por dirección.
     */
    @Test
    void cualquierContraseniaSirve() {
        var pedido = new RegisterRequest("juanca", "juan@example.com", "password123");

        service.register(pedido);

        assertThat(loGuardado().getUsername()).isEqualTo("juanca");
    }

    @Test
    void siElNombreEsDeOtroNoSeCreaLaCuenta() {
        when(userRepository.existsByUsername("juanca")).thenReturn(true);

        assertThatThrownBy(() -> service.register(pidiendo("juanca")))
            .isInstanceOf(UsernameTakenException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    /**
     * Volver a registrarse con una cuenta sin verificar deja cambiar el nombre.
     *
     * Es la misma persona reintentando, y si se ignorara lo que escribió le quedaría el
     * de la primera vuelta sin que nada se lo dijera.
     */
    @Test
    void reintentarSinVerificarDejaCambiarDeNombre() {
        User aMedioHacer = User.builder()
            .id(1L).username("juanca").email("juan@example.com")
            .emailVerified(false).build();
        when(userRepository.findByEmail("juan@example.com")).thenReturn(Optional.of(aMedioHacer));

        service.register(pidiendo("juanca2"));

        assertThat(aMedioHacer.getUsername()).isEqualTo("juanca2");
    }

    /** Reintentar con el mismo no puede chocar contra su propio nombre. */
    @Test
    void reintentarConElMismoNombreNoChocaConsigoMismo() {
        User aMedioHacer = User.builder()
            .id(1L).username("juanca").email("juan@example.com")
            .emailVerified(false).build();
        when(userRepository.findByEmail("juan@example.com")).thenReturn(Optional.of(aMedioHacer));
        when(userRepository.existsByUsername("juanca")).thenReturn(true);

        service.register(pidiendo("JuanCa"));

        assertThat(aMedioHacer.getUsername()).isEqualTo("juanca");
    }
}
