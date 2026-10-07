package com.hamburguesas.service;

import com.hamburguesas.dto.AuthResponse;
import com.hamburguesas.dto.HamburguesaRequest;
import com.hamburguesas.model.User;
import com.hamburguesas.repository.FollowRepository;
import com.hamburguesas.repository.RatingRepository;
import com.hamburguesas.repository.UserRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** La hamburguesa que cada uno elige para su avatar (#151). */
class HamburguesaDelAvatarTest {

    private static final Validator VALIDADOR = Validation.buildDefaultValidatorFactory().getValidator();

    private UserRepository userRepository;
    private ProfileService service;
    private User yo;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        service = new ProfileService(mock(RatingRepository.class), mock(FollowRepository.class), userRepository);
        yo = User.builder().id(7L).username("juanca").email("juanca@example.com").build();
        when(userRepository.findById(7L)).thenReturn(Optional.of(yo));
    }

    /** Lo que se guarda vuelve en la respuesta, que es con lo que el navegador redibuja el avatar. */
    @Test
    void guardaLaQueEligioYLaDevuelve() {
        AuthResponse respuesta = service.cambiarHamburguesa(7L, "20131");

        assertThat(yo.getHamburguesa()).isEqualTo("20131");
        assertThat(respuesta.hamburguesa()).isEqualTo("20131");
        assertThat(respuesta.username()).isEqualTo("juanca");
    }

    @Test
    void sinRecetaVuelveALaQueSaleDelNombre() {
        yo.setHamburguesa("20131");

        AuthResponse respuesta = service.cambiarHamburguesa(7L, null);

        assertThat(yo.getHamburguesa()).isNull();
        assertThat(respuesta.hamburguesa()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"00011", "42233", "20131"})
    void aceptaCincoCifrasDentroDeSuRango(String receta) {
        assertThat(VALIDADOR.validate(new HamburguesaRequest(receta))).isEmpty();
    }

    /** Lo que no se puede dibujar no se guarda: el navegador no tendría qué pintar. */
    @ParameterizedTest
    @ValueSource(strings = {"50131", "23131", "20431", "20141", "20130", "20134", "2013", "201311", "abcde", ""})
    void rechazaLoQueNoSePuedeDibujar(String receta) {
        assertThat(VALIDADOR.validate(new HamburguesaRequest(receta))).isNotEmpty();
    }

    @Test
    void aceptaNadaParaVolverALaDelNombre() {
        assertThat(VALIDADOR.validate(new HamburguesaRequest(null))).isEmpty();
    }
}
