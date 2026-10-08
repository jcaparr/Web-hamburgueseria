package com.hamburguesas.service;

import com.hamburguesas.dto.CuantasReaccionesDto;
import com.hamburguesas.dto.ReaccionesDto;
import com.hamburguesas.exception.ConflictException;
import com.hamburguesas.exception.ResourceNotFoundException;
import com.hamburguesas.model.Rating;
import com.hamburguesas.model.Reaccion;
import com.hamburguesas.model.TipoDeReaccion;
import com.hamburguesas.model.User;
import com.hamburguesas.repository.ReaccionRepository;
import com.hamburguesas.repository.RatingRepository;
import com.hamburguesas.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Reaccionar a la reseña de otro, cambiar de reacción y sacarla (#186). */
class ReaccionesServiceTest {

    private static final Long YO = 1L;
    private static final Long AUTOR = 2L;
    private static final Long RESENIA = 9L;

    private ReaccionRepository reaccionRepository;
    private RatingRepository ratingRepository;
    private Bloqueos bloqueos;
    private Reacciones reacciones;
    private ReaccionesService service;

    @BeforeEach
    void setUp() {
        reaccionRepository = mock(ReaccionRepository.class);
        ratingRepository = mock(RatingRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        bloqueos = mock(Bloqueos.class);
        reacciones = mock(Reacciones.class);

        when(ratingRepository.findById(RESENIA)).thenReturn(Optional.of(
            Rating.builder().id(RESENIA).score(4).user(User.builder().id(AUTOR).build()).build()));
        when(userRepository.getReferenceById(YO)).thenReturn(User.builder().id(YO).build());
        when(reaccionRepository.findByRating_IdAndUser_Id(anyLong(), anyLong())).thenReturn(Optional.empty());

        service = new ReaccionesService(reaccionRepository, ratingRepository, userRepository, bloqueos, reacciones);
    }

    @Test
    void laPrimeraReaccionSeGuarda() {
        service.reaccionar(RESENIA, YO, TipoDeReaccion.HAMBRE);

        ArgumentCaptor<Reaccion> guardada = ArgumentCaptor.forClass(Reaccion.class);
        verify(reaccionRepository).save(guardada.capture());
        assertThat(guardada.getValue().getTipo()).isEqualTo(TipoDeReaccion.HAMBRE);
        assertThat(guardada.getValue().getUser().getId()).isEqualTo(YO);
    }

    /** Hay una por persona: elegir otra cambia la que había, no suma una segunda. */
    @Test
    void elegirOtraReemplazaALaQueHabia() {
        Reaccion ya = Reaccion.builder().id(5L).tipo(TipoDeReaccion.HAMBRE).build();
        when(reaccionRepository.findByRating_IdAndUser_Id(RESENIA, YO)).thenReturn(Optional.of(ya));

        service.reaccionar(RESENIA, YO, TipoDeReaccion.FUEGO);

        assertThat(ya.getTipo()).isEqualTo(TipoDeReaccion.FUEGO);
        verify(reaccionRepository, never()).save(any());
    }

    /** Devuelve cómo quedaron, contadas de nuevo, para que la pantalla se ponga al día. */
    @Test
    void devuelveComoQuedaronLasDeEsaResenia() {
        ReaccionesDto quedaron = new ReaccionesDto(
            List.of(new CuantasReaccionesDto(TipoDeReaccion.FUEGO, 4)), TipoDeReaccion.FUEGO);
        when(reacciones.de(List.of(RESENIA), YO)).thenReturn(Map.of(RESENIA, quedaron));

        assertThat(service.reaccionar(RESENIA, YO, TipoDeReaccion.FUEGO)).isEqualTo(quedaron);
    }

    @Test
    void aLaPropiaNoSePuede() {
        assertThatThrownBy(() -> service.reaccionar(RESENIA, AUTOR, TipoDeReaccion.APLAUSO))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("propia");

        verify(reaccionRepository, never()).save(any());
    }

    /** Con un bloqueo de por medio, la reseña no existe para quien quiere reaccionar. */
    @Test
    void conUnBloqueoDeMedioEsComoSiNoExistiera() {
        when(bloqueos.hayEntre(YO, AUTOR)).thenReturn(true);

        assertThatThrownBy(() -> service.reaccionar(RESENIA, YO, TipoDeReaccion.RISA))
            .isInstanceOf(ResourceNotFoundException.class);

        verify(reaccionRepository, never()).save(any());
    }

    @Test
    void aUnaQueNoExisteDa404() {
        assertThatThrownBy(() -> service.reaccionar(77L, YO, TipoDeReaccion.RISA))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void sacarlaLaBorra() {
        service.sacar(RESENIA, YO);

        verify(reaccionRepository).deleteByRating_IdAndUser_Id(RESENIA, YO);
    }
}
