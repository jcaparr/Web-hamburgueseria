package com.hamburguesas.service;

import com.hamburguesas.dto.AuthResponse;
import com.hamburguesas.dto.EstadoDelNombreDto;
import com.hamburguesas.exception.UsernameCooldownException;
import com.hamburguesas.exception.UsernameTakenException;
import com.hamburguesas.model.User;
import com.hamburguesas.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Cambiar el nombre de usuario, una vez cada 30 días (#380). */
class CambioDeNombreDeUsuarioTest {

    private static final ZoneId BA = ZoneId.of("America/Argentina/Buenos_Aires");

    private UserRepository userRepository;
    private User yo;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        yo = User.builder().id(7L).username("juanca").email("juanca@example.com").build();
        when(userRepository.bloquear(7L)).thenReturn(Optional.of(yo));
        when(userRepository.findById(7L)).thenReturn(Optional.of(yo));
    }

    private static Instant en(int anio, int mes, int dia, int hora, int minuto) {
        return ZonedDateTime.of(anio, mes, dia, hora, minuto, 0, 0, BA).toInstant();
    }

    private CambioDeNombreDeUsuario conReloj(Instant ahora) {
        return new CambioDeNombreDeUsuario(
            userRepository, new UsernameService(userRepository), Clock.fixed(ahora, BA));
    }

    @Test
    void cambiaElNombreYAnotaCuando() {
        Instant ahora = en(2026, 11, 2, 10, 0);

        AuthResponse respuesta = conReloj(ahora).cambiar(7L, "  JuanNuevo ");

        assertThat(yo.getUsername()).isEqualTo("juannuevo");
        assertThat(yo.getUsernameChangedAt()).isEqualTo(ahora);
        assertThat(respuesta.username()).isEqualTo("juannuevo");
    }

    @Test
    void sinCambioPrevioPuedeCambiarYa() {
        assertThat(conReloj(en(2026, 11, 2, 10, 0)).estado(7L))
            .isEqualTo(new EstadoDelNombreDto(null, null));
    }

    @Test
    void unNombreEnUsoSeRechazaYNoGastaElPlazo() {
        when(userRepository.existsByUsername("ocupado")).thenReturn(true);

        assertThatThrownBy(() -> conReloj(en(2026, 11, 2, 10, 0)).cambiar(7L, "ocupado"))
            .isInstanceOf(UsernameTakenException.class);

        assertThat(yo.getUsername()).isEqualTo("juanca");
        assertThat(yo.getUsernameChangedAt()).isNull();
    }

    /** Mayúsculas distintas son el mismo nombre. */
    @Test
    void unNombreEnUsoConOtrasMayusculasSeRechaza() {
        when(userRepository.existsByUsername("ocupado")).thenReturn(true);

        assertThatThrownBy(() -> conReloj(en(2026, 11, 2, 10, 0)).cambiar(7L, "OCUPADO"))
            .isInstanceOf(UsernameTakenException.class);
    }

    @Test
    void unNombreReservadoOMalFormadoSeRechaza() {
        CambioDeNombreDeUsuario servicio = conReloj(en(2026, 11, 2, 10, 0));

        assertThatThrownBy(() -> servicio.cambiar(7L, "admin")).isInstanceOf(UsernameTakenException.class);
        assertThatThrownBy(() -> servicio.cambiar(7L, "a b")).isInstanceOf(UsernameTakenException.class);
        assertThatThrownBy(() -> servicio.cambiar(7L, "ab")).isInstanceOf(UsernameTakenException.class);
        assertThat(yo.getUsername()).isEqualTo("juanca");
    }

    @Test
    void antesDeLos30DiasSeRechazaConLaFechaEnQuePuede() {
        yo.setUsernameChangedAt(en(2026, 11, 1, 12, 0));

        assertThatThrownBy(() -> conReloj(en(2026, 11, 20, 12, 0)).cambiar(7L, "otro")).satisfies(e -> {
            assertThat(e).isInstanceOf(UsernameCooldownException.class);
            assertThat(((UsernameCooldownException) e).getPuedeCambiarDesde()).isEqualTo(LocalDate.of(2026, 12, 1));
            assertThat(e.getMessage()).contains("1 de diciembre");
        });
        assertThat(yo.getUsername()).isEqualTo("juanca");
    }

    /** Lo cambió el 1/11 a las 23:30: el día 30 después es el 1/12 y recién a las 00:00 vale. */
    @Test
    void elBordeDeLos30DiasSeCuentaEnHoraDeBuenosAires() {
        yo.setUsernameChangedAt(en(2026, 11, 1, 23, 30));

        // 30/11 23:59 todavía no.
        assertThatThrownBy(() -> conReloj(en(2026, 11, 30, 23, 59)).cambiar(7L, "otro"))
            .isInstanceOf(UsernameCooldownException.class);
        // 1/12 00:00 ya sí.
        AuthResponse respuesta = conReloj(en(2026, 12, 1, 0, 0)).cambiar(7L, "otro");
        assertThat(respuesta.username()).isEqualTo("otro");
    }

    /** Domingo 23:30 y lunes 00:30 de Buenos Aires son días distintos aunque en UTC sean el mismo. */
    @Test
    void elDiaSeCuentaEnBuenosAiresYNoEnUtc() {
        // 2026-11-01 22:00 BA = 2026-11-02 01:00 UTC.
        yo.setUsernameChangedAt(en(2026, 11, 1, 22, 0));

        assertThat(conReloj(en(2026, 11, 2, 0, 30)).estado(7L).puedeCambiarDesde())
            .isEqualTo(LocalDate.of(2026, 12, 1));
    }

    @Test
    void pedirElMismoNombreNoEsUnCambio() {
        yo.setUsernameChangedAt(en(2026, 11, 1, 12, 0));

        AuthResponse respuesta = conReloj(en(2026, 11, 2, 12, 0)).cambiar(7L, "JuanCa");

        assertThat(respuesta.username()).isEqualTo("juanca");
        assertThat(yo.getUsernameChangedAt()).isEqualTo(en(2026, 11, 1, 12, 0));
        verify(userRepository, never()).saveAndFlush(any());
    }

    /** Dos pedidos a la vez por el mismo nombre: el que pierde en la base recibe "en uso", no un 500. */
    @Test
    void siLaBaseDiceQueYaEstaTomadoSeContestaEnUso() {
        when(userRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("uk_users_username"));

        assertThatThrownBy(() -> conReloj(en(2026, 11, 2, 10, 0)).cambiar(7L, "carrera"))
            .isInstanceOf(UsernameTakenException.class);
    }

    @Test
    void elNombreViejoQuedaLibreEnElActo() {
        conReloj(en(2026, 11, 2, 10, 0)).cambiar(7L, "juannuevo");

        // El servicio no guarda el viejo en ningún lado: lo único que vale es lo que dice la base.
        verify(userRepository).saveAndFlush(yo);
        assertThat(yo.getUsername()).isNotEqualTo("juanca");
    }
}
