package com.hamburguesas.service;

import com.hamburguesas.exception.ConflictException;
import com.hamburguesas.exception.ResourceNotFoundException;
import com.hamburguesas.model.Block;
import com.hamburguesas.model.User;
import com.hamburguesas.repository.BlockRepository;
import com.hamburguesas.repository.FollowRepository;
import com.hamburguesas.repository.RatingRepository;
import com.hamburguesas.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Bloquear a alguien.
 *
 * Lo que se afirma acá es sobre todo qué deja de pasar: que no lo siga, que no lo
 * encuentre, que no llegue a su perfil, y que enterarse de que fue bloqueado no sea
 * parte de la respuesta.
 */
class BloqueoTest {

    private static final Long YO = 1L;
    private static final Long MOLESTO = 2L;

    private UserRepository userRepository;
    private FollowRepository followRepository;
    private BlockRepository blockRepository;
    private Bloqueos bloqueos;
    private SocialService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        followRepository = mock(FollowRepository.class);
        blockRepository = mock(BlockRepository.class);
        bloqueos = mock(Bloqueos.class);
        RatingRepository ratingRepository = mock(RatingRepository.class);

        when(userRepository.findByUsername("molesto"))
            .thenReturn(Optional.of(persona(MOLESTO, "molesto")));
        when(userRepository.getReferenceById(YO)).thenReturn(persona(YO, "yo"));
        when(userRepository.findByUsername("yo")).thenReturn(Optional.of(persona(YO, "yo")));
        when(ratingRepository.ultimasDe(anyLong(), any())).thenReturn(List.of());
        when(userRepository.buscarPorNombreDeUsuario(anyString(), anyString(), anyCollection(), any()))
            .thenReturn(List.of());
        when(bloqueos.hayEntre(any(), any())).thenReturn(false);
        when(bloqueos.queNoPuedeVerNiASiMismo(any())).thenReturn(List.of(-1L));

        service = new SocialService(
            userRepository, followRepository, ratingRepository, blockRepository, bloqueos);
    }

    private User persona(Long id, String username) {
        return User.builder().id(id).username(username).email(username + "@example.com").build();
    }

    /** Que ya exista un bloqueo entre los dos, lo haya puesto cualquiera. */
    private void bloqueadosEntreSi() {
        when(bloqueos.hayEntre(YO, MOLESTO)).thenReturn(true);
        when(bloqueos.hayEntre(MOLESTO, YO)).thenReturn(true);
    }

    @Test
    void bloquearGuardaQuienBloqueoAQuien() {
        service.bloquear("molesto", YO);

        ArgumentCaptor<Block> guardado = ArgumentCaptor.forClass(Block.class);
        verify(blockRepository).save(guardado.capture());
        assertThat(guardado.getValue().getBlocker().getId()).isEqualTo(YO);
        assertThat(guardado.getValue().getBlocked().getId()).isEqualTo(MOLESTO);
    }

    /**
     * Y corta el seguir en las dos direcciones.
     *
     * Dejar en pie que el bloqueado lo siga sería dejarlo justo donde molestaba: en su
     * feed, apareciendo cada vez que reseña algo.
     */
    @Test
    void bloquearCortaElSeguirEnLasDosDirecciones() {
        service.bloquear("molesto", YO);

        verify(followRepository).deleteByFollower_IdAndFollowed_Id(YO, MOLESTO);
        verify(followRepository).deleteByFollower_IdAndFollowed_Id(MOLESTO, YO);
    }

    @Test
    void bloquearDosVecesNoGuardaDosVeces() {
        when(blockRepository.existsByBlocker_IdAndBlocked_Id(YO, MOLESTO)).thenReturn(true);

        service.bloquear("molesto", YO);

        verify(blockRepository, never()).save(any());
    }

    @Test
    void nadieSeBloqueaASiMismo() {
        assertThatThrownBy(() -> service.bloquear("yo", YO))
            .isInstanceOf(ConflictException.class);

        verify(blockRepository, never()).save(any());
    }

    /**
     * Con un bloqueo de por medio el perfil contesta lo mismo que si no existiera.
     *
     * Un "esta persona te bloqueó" le confirmaría al bloqueado que lo bloquearon, que
     * es justo el aviso que hace que insista por otro lado.
     */
    @Test
    void elPerfilDeAlguienBloqueadoNoSeEncuentra() {
        bloqueadosEntreSi();

        assertThatThrownBy(() -> service.perfil("molesto", YO))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("No encontramos a esa persona");
    }

    /** Y tampoco se lo puede seguir, con el mismo silencio. */
    @Test
    void aAlguienBloqueadoNoSeLoPuedeSeguir() {
        bloqueadosEntreSi();

        assertThatThrownBy(() -> service.seguir("molesto", YO))
            .isInstanceOf(ResourceNotFoundException.class);

        verify(followRepository, never()).save(any());
    }

    /** El propio perfil se ve siempre, aunque uno se haya bloqueado con medio mundo. */
    @Test
    void elPerfilPropioSeVeIgual() {
        when(bloqueos.hayEntre(any(), any())).thenReturn(true);

        assertThat(service.perfil("yo", YO).soyYo()).isTrue();
    }

    @Test
    void desbloquearBorraElBloqueo() {
        service.desbloquear("molesto", YO);

        verify(blockRepository).deleteByBlocker_IdAndBlocked_Id(YO, MOLESTO);
    }

    /**
     * Desbloquear no devuelve los seguimientos que el bloqueo cortó.
     *
     * Volver a seguir es una decisión, y tomarla de nuevo por alguien porque una vez la
     * tomó sería raro: puede estar desbloqueando solo para sacarlo de la lista.
     */
    @Test
    void desbloquearNoDevuelveLosSeguimientos() {
        service.desbloquear("molesto", YO);

        verify(followRepository, never()).save(any());
    }

    /** La lista es el único lugar donde vuelve a ver ese nombre, para poder deshacerlo. */
    @Test
    void laListaDeBloqueadosDiceAQuienYCuando() {
        Instant cuando = Instant.parse("2026-09-25T12:00:00Z");
        when(blockRepository.findByBlocker_IdOrderByCreatedAtDesc(YO)).thenReturn(List.of(
            Block.builder().id(1L).blocker(persona(YO, "yo"))
                .blocked(persona(MOLESTO, "molesto")).createdAt(cuando).build()));

        assertThat(service.bloqueados(YO))
            .singleElement()
            .satisfies(b -> {
                assertThat(b.username()).isEqualTo("molesto");
                assertThat(b.bloqueadoEl()).isEqualTo(cuando);
            });
    }
}
