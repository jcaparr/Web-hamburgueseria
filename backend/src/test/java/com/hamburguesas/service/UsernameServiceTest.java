package com.hamburguesas.service;

import com.hamburguesas.exception.UsernameTakenException;
import com.hamburguesas.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Los nombres de usuario contra lo que ya está tomado. */
class UsernameServiceTest {

    private UserRepository userRepository;
    private UsernameService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        service = new UsernameService(userRepository);
    }

    /** Lo tomado son estos, y nada más. */
    private void yaExisten(String... nombres) {
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        for (String nombre : nombres) {
            when(userRepository.existsByUsername(nombre)).thenReturn(true);
        }
    }

    @Test
    void guardaEnMinusculasLoQueSeEscribioComoSea() {
        yaExisten();

        assertThat(service.reservar("JuanCa")).isEqualTo("juanca");
    }

    @Test
    void noDejaTomarElDeOtro() {
        yaExisten("juanca");

        assertThatThrownBy(() -> service.reservar("JuanCa"))
            .isInstanceOf(UsernameTakenException.class)
            .hasMessageContaining("ya está en uso");
    }

    /**
     * Un nombre reservado se rechaza con el mismo mensaje que uno tomado.
     *
     * Decir "ese está reservado" no le sirve de nada a quien lo pidió —la salida es la
     * misma, elegir otro— y a cambio le confirma a cualquiera cuáles son.
     */
    @Test
    void unNombreReservadoSeRechazaComoSiEstuvieraTomado() {
        yaExisten();

        assertThatThrownBy(() -> service.reservar("soporte"))
            .isInstanceOf(UsernameTakenException.class)
            .hasMessageContaining("ya está en uso");
    }

    @Test
    void unNombreMalFormadoSeRechazaDiciendoQueSeAcepta() {
        yaExisten();

        assertThatThrownBy(() -> service.reservar("ju"))
            .isInstanceOf(UsernameTakenException.class)
            .hasMessageContaining("Entre 3 y 20");
    }

    @Test
    void siEstaLibreLoSugiereTalCual() {
        yaExisten();

        assertThat(service.sugerirCerca("juanca")).isEqualTo("juanca");
    }

    @Test
    void siEstaTomadoSugiereElPrimeroLibreConNumero() {
        yaExisten("juanca", "juanca2");

        assertThat(service.sugerirCerca("juanca")).isEqualTo("juanca3");
    }

    /**
     * Si ni con veinte variantes se encontró una libre, no se devuelve "juanca173": a
     * esa altura conviene que elija otro nombre antes que quedarse con un número.
     */
    @Test
    void siNoHayNingunaCercaNoSugiereNada() {
        when(userRepository.existsByUsername(anyString())).thenReturn(true);

        assertThat(service.sugerirCerca("juanca")).isEmpty();
    }

    /** Lo que se sugiere tiene que poder usarse, no solo estar libre. */
    @Test
    void loQueSugiereSiempreSirve() {
        yaExisten();

        assertThat(service.sugerirCerca("SOPORTE")).isEqualTo("soporte2");
    }
}
