package com.hamburguesas.service;

import com.hamburguesas.auth.GoogleTokenVerifier;
import com.hamburguesas.dto.GoogleLoginRequest;
import com.hamburguesas.exception.NeedsUsernameException;
import com.hamburguesas.exception.UsernameTakenException;
import com.hamburguesas.model.User;
import com.hamburguesas.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
 * Entrar con Google la primera vez, cuando todavía no eligió cómo lo van a encontrar.
 *
 * Lo que se afirma acá es sobre todo lo que <em>no</em> pasa: que no quede una cuenta
 * creada hasta que el nombre esté elegido.
 */
class GoogleConNombreDeUsuarioTest {

    private static final String TOKEN = "el-token-que-da-google";

    private UserRepository userRepository;
    private GoogleAuthService service;

    @BeforeEach
    void setUp() {
        GoogleTokenVerifier verifier = mock(GoogleTokenVerifier.class);
        userRepository = mock(UserRepository.class);

        when(verifier.isConfigured()).thenReturn(true);
        when(verifier.verify(TOKEN)).thenReturn(Optional.of(
            new GoogleTokenVerifier.GoogleAccount("sub-123", "juan.perez@gmail.com")));
        when(userRepository.findByGoogleSub(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(llamada -> llamada.getArgument(0));

        service = new GoogleAuthService(verifier, userRepository, new UsernameService(userRepository));
    }

    @Test
    void laPrimeraVezPideElegirUnNombre() {
        assertThatThrownBy(() -> service.login(new GoogleLoginRequest(TOKEN, null)))
            .isInstanceOf(NeedsUsernameException.class);
    }

    /**
     * Y mientras tanto no se creó nada. Si la cuenta se fuera creando igual, cerrar la
     * pestaña en esa pantalla dejaría una cuenta sin nombre, imposible de mostrar.
     */
    @Test
    void yHastaEntoncesNoSeCreaLaCuenta() {
        assertThatThrownBy(() -> service.login(new GoogleLoginRequest(TOKEN, null)))
            .isInstanceOf(NeedsUsernameException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void laSugerenciaSaleDelEmailYEstaLibre() {
        assertThatThrownBy(() -> service.login(new GoogleLoginRequest(TOKEN, null)))
            .isInstanceOfSatisfying(NeedsUsernameException.class,
                ex -> assertThat(ex.getSuggestion()).isEqualTo("juanperez"));
    }

    @Test
    void siLaSugerenciaObviaEstaTomadaProponeOtra() {
        when(userRepository.existsByUsername("juanperez")).thenReturn(true);

        assertThatThrownBy(() -> service.login(new GoogleLoginRequest(TOKEN, null)))
            .isInstanceOfSatisfying(NeedsUsernameException.class,
                ex -> assertThat(ex.getSuggestion()).isEqualTo("juanperez2"));
    }

    @Test
    void conElNombreElegidoSeCreaLaCuenta() {
        User creado = service.login(new GoogleLoginRequest(TOKEN, "JuanCa"));

        assertThat(creado.getUsername()).isEqualTo("juanca");
        assertThat(creado.getEmail()).isEqualTo("juan.perez@gmail.com");
        assertThat(creado.isEmailVerified()).isTrue();
    }

    @Test
    void siEnElInterinAlguienSeLoLlevoNoSeCreaLaCuenta() {
        when(userRepository.existsByUsername("juanca")).thenReturn(true);

        assertThatThrownBy(() -> service.login(new GoogleLoginRequest(TOKEN, "juanca")))
            .isInstanceOf(UsernameTakenException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    /** Quien ya tiene cuenta entra de una: el nombre lo eligió la primera vez. */
    @Test
    void aQuienYaTieneCuentaNoSeLeVuelveAPedir() {
        User existente = User.builder()
            .id(3L).username("juanca").email("juan.perez@gmail.com")
            .googleSub("sub-123").emailVerified(true).build();
        when(userRepository.findByGoogleSub("sub-123")).thenReturn(Optional.of(existente));

        assertThat(service.login(new GoogleLoginRequest(TOKEN, null))).isSameAs(existente);
    }
}
