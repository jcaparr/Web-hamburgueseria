package com.hamburguesas.service;

import com.hamburguesas.dto.ReseniasPorUsuarioDto;
import com.hamburguesas.exception.ConflictException;
import com.hamburguesas.exception.ResourceNotFoundException;
import com.hamburguesas.model.Follow;
import com.hamburguesas.model.User;
import com.hamburguesas.repository.BlockRepository;
import com.hamburguesas.repository.FollowRepository;
import com.hamburguesas.repository.RatingRepository;
import com.hamburguesas.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Buscar gente, mirar su perfil y seguirla. */
class SocialServiceTest {

    private static final Long YO = 1L;
    private static final Long OTRO = 2L;

    private UserRepository userRepository;
    private FollowRepository followRepository;
    private RatingRepository ratingRepository;
    private BlockRepository blockRepository;
    private Bloqueos bloqueos;
    private SocialService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        followRepository = mock(FollowRepository.class);
        ratingRepository = mock(RatingRepository.class);
        blockRepository = mock(BlockRepository.class);
        bloqueos = mock(Bloqueos.class);

        when(userRepository.buscarPorNombreDeUsuario(anyString(), anyString(), any(), any()))
            .thenReturn(List.of());
        when(followRepository.idsQueSigueDeEntre(anyLong(), anyCollection())).thenReturn(List.of());
        when(ratingRepository.contarPorUsuario(anyCollection())).thenReturn(List.of());
        when(ratingRepository.ultimasDe(anyLong(), any())).thenReturn(List.of());
        when(bloqueos.queNoPuedeVerNiASiMismo(any())).thenReturn(List.of(-1L));
        when(bloqueos.hayEntre(any(), any())).thenReturn(false);

        service = new SocialService(
            userRepository, followRepository, ratingRepository, blockRepository, bloqueos);
    }

    private User persona(Long id, String username) {
        return User.builder().id(id).username(username).email(username + "@example.com").build();
    }

    /** Lo que de verdad se le pasó a la base. */
    private String patronBuscado() {
        ArgumentCaptor<String> patron = ArgumentCaptor.forClass(String.class);
        verify(userRepository).buscarPorNombreDeUsuario(
            patron.capture(), anyString(), any(), any(Pageable.class));
        return patron.getValue();
    }

    @Test
    void buscaPorPedazoDeNombre() {
        service.buscar("juan", YO);

        assertThat(patronBuscado()).isEqualTo("%juan%");
    }

    @Test
    void buscarNoDistingueMayusculas() {
        service.buscar("JuanCa", YO);

        assertThat(patronBuscado()).isEqualTo("%juanca%");
    }

    /**
     * El guión bajo se escapa antes de llegar a la consulta.
     *
     * Es parte de un nombre válido y a la vez vale por cualquier carácter en un LIKE:
     * sin escaparlo, quien busca a juan_ca encontraría también a juanXca.
     */
    @Test
    void elGuionBajoSeBuscaComoGuionBajoYNoComoComodin() {
        service.buscar("juan_ca", YO);

        assertThat(patronBuscado()).isEqualTo("%juan\\_ca%");
    }

    /**
     * Lo que no puede haber en un nombre no llega a la consulta.
     *
     * No es lo que la hace segura —va parametrizada— pero buscar por algo que ningún
     * nombre puede contener no puede devolver nada, así que se saca antes.
     */
    @Test
    void loQueNingunNombrePuedeTenerSeDescarta() {
        service.buscar("ju%an'; drop", YO);

        assertThat(patronBuscado()).isEqualTo("%juandrop%");
    }

    /** Con una sola letra se devolvería medio padrón, así que no se busca. */
    @Test
    void conMenosDeDosLetrasNoSeBusca() {
        assertThat(service.buscar("j", YO)).isEmpty();

        verify(userRepository, never()).buscarPorNombreDeUsuario(
            anyString(), anyString(), any(), any());
    }

    /** Y lo mismo si de lo escrito no queda nada usable. */
    @Test
    void siDeLoEscritoNoQuedaNadaTampocoSeBusca() {
        assertThat(service.buscar("%%%", YO)).isEmpty();

        verify(userRepository, never()).buscarPorNombreDeUsuario(
            anyString(), anyString(), any(), any());
    }

    /**
     * Lo que no tiene que salir se pide en un solo lugar y se pasa tal cual.
     *
     * Uno mismo y los bloqueados son la misma cosa desde acá —gente que no va en esta
     * lista— y por eso viajan juntos: que el buscador arme su propia idea de a quién
     * esconder es como se termina escondiendo a alguien en el feed y no en la búsqueda.
     */
    @Test
    void loQueNoTieneQueSalirLoDecideBloqueos() {
        when(bloqueos.queNoPuedeVerNiASiMismo(YO)).thenReturn(List.of(YO, 44L));

        service.buscar("juan", YO);

        verify(userRepository).buscarPorNombreDeUsuario(
            anyString(), anyString(), eq(List.of(YO, 44L)), any(Pageable.class));
    }

    @Test
    void losResultadosDicenACualesYaSeguis() {
        when(userRepository.buscarPorNombreDeUsuario(anyString(), anyString(), any(), any()))
            .thenReturn(List.of(persona(2L, "juanca"), persona(3L, "juanpe")));
        when(followRepository.idsQueSigueDeEntre(YO, List.of(2L, 3L))).thenReturn(List.of(3L));

        assertThat(service.buscar("juan", YO))
            .extracting("username", "loSigo")
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple("juanca", false),
                org.assertj.core.groups.Tuple.tuple("juanpe", true));
    }

    /** Quien no tiene reseñas no vuelve del group by, y tiene que quedar en cero. */
    @Test
    void quienNoTieneReseniasFiguraEnCero() {
        when(userRepository.buscarPorNombreDeUsuario(anyString(), anyString(), any(), any()))
            .thenReturn(List.of(persona(2L, "juanca"), persona(3L, "juanpe")));
        when(ratingRepository.contarPorUsuario(List.of(2L, 3L)))
            .thenReturn(List.of(new ReseniasPorUsuarioDto(3L, 4L)));

        assertThat(service.buscar("juan", YO))
            .extracting("username", "resenias")
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple("juanca", 0L),
                org.assertj.core.groups.Tuple.tuple("juanpe", 4L));
    }

    @Test
    void seguirAAlguienGuardaLaRelacionEnEseSentido() {
        when(userRepository.findByUsername("juanca")).thenReturn(Optional.of(persona(OTRO, "juanca")));
        when(userRepository.getReferenceById(YO)).thenReturn(persona(YO, "yo"));

        service.seguir("juanca", YO);

        ArgumentCaptor<Follow> guardado = ArgumentCaptor.forClass(Follow.class);
        verify(followRepository).save(guardado.capture());
        assertThat(guardado.getValue().getFollower().getId()).isEqualTo(YO);
        assertThat(guardado.getValue().getFollowed().getId()).isEqualTo(OTRO);
    }

    /** Seguir de nuevo a quien ya seguís no es un error: ya estabas donde querías estar. */
    @Test
    void seguirDosVecesNoGuardaDosVeces() {
        when(userRepository.findByUsername("juanca")).thenReturn(Optional.of(persona(OTRO, "juanca")));
        when(followRepository.existsByFollower_IdAndFollowed_Id(YO, OTRO)).thenReturn(true);

        service.seguir("juanca", YO);

        verify(followRepository, never()).save(any());
    }

    @Test
    void nadieSeSigueASiMismo() {
        when(userRepository.findByUsername("yo")).thenReturn(Optional.of(persona(YO, "yo")));

        assertThatThrownBy(() -> service.seguir("yo", YO))
            .isInstanceOf(ConflictException.class);

        verify(followRepository, never()).save(any());
    }

    @Test
    void seguirAAlguienQueNoExisteEs404() {
        when(userRepository.findByUsername("fantasma")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.seguir("fantasma", YO))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void elPerfilPropioNoOfreceSeguirse() {
        when(userRepository.findByUsername("yo")).thenReturn(Optional.of(persona(YO, "yo")));

        var perfil = service.perfil("yo", YO);

        assertThat(perfil.soyYo()).isTrue();
        assertThat(perfil.loSigo()).isFalse();
    }

    /**
     * El perfil de otro no lleva su email.
     *
     * Es lo único que hay que sostener acá: el DTO se arma campo por campo justamente
     * para que lo que no esté en él no pueda salir por este endpoint.
     */
    @Test
    void elPerfilDeOtroNoLlevaSuEmail() {
        when(userRepository.findByUsername("juanca")).thenReturn(Optional.of(persona(OTRO, "juanca")));

        var perfil = service.perfil("juanca", YO);

        assertThat(perfil.username()).isEqualTo("juanca");
        assertThat(perfil.toString()).doesNotContain("@example.com");
    }

    @Test
    void elPerfilDiceSiYaLoSeguis() {
        when(userRepository.findByUsername("juanca")).thenReturn(Optional.of(persona(OTRO, "juanca")));
        when(followRepository.existsByFollower_IdAndFollowed_Id(YO, OTRO)).thenReturn(true);

        assertThat(service.perfil("juanca", YO).loSigo()).isTrue();
    }

    /** Se puede llegar a un perfil escribiendo el nombre con mayúsculas. */
    @Test
    void alPerfilSeLlegaEscribaComoEscriba() {
        when(userRepository.findByUsername("juanca")).thenReturn(Optional.of(persona(OTRO, "juanca")));

        assertThat(service.perfil("JuanCa", YO).username()).isEqualTo("juanca");
    }

    /** Dejar de seguir a quien no seguías tampoco es un error. */
    @Test
    void dejarDeSeguirAQuienNoSeguiasNoFalla() {
        when(userRepository.findByUsername("juanca")).thenReturn(Optional.of(persona(OTRO, "juanca")));
        when(followRepository.deleteByFollower_IdAndFollowed_Id(YO, OTRO)).thenReturn(0L);

        service.dejarDeSeguir("juanca", YO);

        verify(followRepository).deleteByFollower_IdAndFollowed_Id(YO, OTRO);
    }
}
