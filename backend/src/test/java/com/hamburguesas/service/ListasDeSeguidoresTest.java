package com.hamburguesas.service;

import com.hamburguesas.dto.ReseniasPorUsuarioDto;
import com.hamburguesas.dto.UsuarioBuscadoDto;
import com.hamburguesas.exception.ResourceNotFoundException;
import com.hamburguesas.model.User;
import com.hamburguesas.repository.BlockRepository;
import com.hamburguesas.repository.FollowRepository;
import com.hamburguesas.repository.RatingRepository;
import com.hamburguesas.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Ver quiénes siguen a alguien y a quiénes sigue (#183). */
class ListasDeSeguidoresTest {

    private static final Long YO = 1L;
    private static final Long ELLA = 2L;
    private static final List<Long> OCULTOS = List.of(9L);

    private UserRepository userRepository;
    private FollowRepository followRepository;
    private RatingRepository ratingRepository;
    private Bloqueos bloqueos;
    private SocialService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        followRepository = mock(FollowRepository.class);
        ratingRepository = mock(RatingRepository.class);
        bloqueos = mock(Bloqueos.class);

        when(userRepository.findByUsername("ella")).thenReturn(Optional.of(persona(ELLA, "ella")));
        when(userRepository.findByUsername("yo")).thenReturn(Optional.of(persona(YO, "yo")));
        when(bloqueos.queNoPuedeVer(YO)).thenReturn(OCULTOS);
        when(followRepository.idsQueSigueDeEntre(anyLong(), anyCollection())).thenReturn(List.of());
        when(ratingRepository.contarPorUsuario(anyCollection())).thenReturn(List.of());

        service = new SocialService(
            userRepository, followRepository, ratingRepository, mock(BlockRepository.class), bloqueos);
    }

    private static User persona(Long id, String username) {
        return User.builder().id(id).username(username).email(username + "@example.com").build();
    }

    @Test
    void losSeguidoresVienenConSusReseniasYSiYaLosSigo() {
        when(followRepository.seguidoresDe(ELLA, OCULTOS)).thenReturn(List.of(
            persona(5L, "ana"), persona(6L, "beto")));
        when(followRepository.idsQueSigueDeEntre(YO, List.of(5L, 6L))).thenReturn(List.of(6L));
        when(ratingRepository.contarPorUsuario(List.of(5L, 6L)))
            .thenReturn(List.of(new ReseniasPorUsuarioDto(5L, 3L)));

        assertThat(service.seguidores("ella", YO)).containsExactly(
            new UsuarioBuscadoDto(5L, "ana", null, 3, false),
            new UsuarioBuscadoDto(6L, "beto", null, 0, true));
    }

    @Test
    void aQuienesSigue() {
        when(followRepository.seguidosPor(ELLA, OCULTOS)).thenReturn(List.of(persona(7L, "carla")));

        assertThat(service.siguiendo("ella", YO)).extracting(UsuarioBuscadoDto::username)
            .containsExactly("carla");
    }

    /** Las dos listas salen sin la gente con la que quien mira tiene un bloqueo. */
    @Test
    void lasListasNoMuestranALosBloqueados() {
        service.seguidores("ella", YO);
        service.siguiendo("ella", YO);

        verify(followRepository).seguidoresDe(ELLA, OCULTOS);
        verify(followRepository).seguidosPor(ELLA, OCULTOS);
    }

    /** Con un bloqueo de por medio, la persona no existe: tampoco sus listas. */
    @Test
    void lasDeAlguienConQuienHayUnBloqueoSon404() {
        when(bloqueos.hayEntre(YO, ELLA)).thenReturn(true);

        assertThatThrownBy(() -> service.seguidores("ella", YO)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.siguiendo("ella", YO)).isInstanceOf(ResourceNotFoundException.class);
        verify(followRepository, never()).seguidoresDe(any(), any());
    }

    @Test
    void lasPropiasSeVenSiempre() {
        when(followRepository.seguidoresDe(YO, OCULTOS)).thenReturn(List.of(persona(5L, "ana")));

        assertThat(service.seguidores("yo", YO)).hasSize(1);
    }

    @Test
    void deAlguienQueNoExisteEs404() {
        assertThatThrownBy(() -> service.seguidores("nadie", YO)).isInstanceOf(ResourceNotFoundException.class);
    }
}
