package com.hamburguesas.service;

import com.hamburguesas.dto.NotaYComentarioDto;
import com.hamburguesas.dto.NotaYCuantasDto;
import com.hamburguesas.dto.RatingResponse;
import com.hamburguesas.dto.ResumenDeReseniasDto;
import com.hamburguesas.fotos.FotosDeResenias;
import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.model.Rating;
import com.hamburguesas.model.User;
import com.hamburguesas.repository.BurgerJointRepository;
import com.hamburguesas.repository.FollowRepository;
import com.hamburguesas.repository.RatingRepository;
import com.hamburguesas.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Las reseñas de una hamburguesería: la lista y lo que va arriba de la lista.
 *
 * Lo que se prueba acá es lo que no se ve mirando las consultas: que las cinco barras
 * estén aunque la base devuelva dos, y que un bloqueo valga tanto en esta pantalla como
 * en el feed y en el buscador.
 */
class ReseniasDeUnLocalTest {

    private static final Long YO = 1L;
    private static final Long LOCAL = 5L;
    private static final Pageable PRIMERA_PAGINA = PageRequest.of(0, 20);

    /** Lo que devuelve Bloqueos cuando no hay a nadie que esconder. */
    private static final List<Long> NADIE = List.of(-1L);

    private RatingRepository ratingRepository;
    private FollowRepository followRepository;
    private Bloqueos bloqueos;
    private RatingService service;

    @BeforeEach
    void setUp() {
        ratingRepository = mock(RatingRepository.class);
        followRepository = mock(FollowRepository.class);
        bloqueos = mock(Bloqueos.class);

        when(ratingRepository.distribucionDeNotas(LOCAL)).thenReturn(List.of());
        when(ratingRepository.deUnLocalSalvo(any(), any(), any())).thenReturn(Page.empty());
        when(followRepository.idsQueSigue(anyLong())).thenReturn(List.of());
        when(bloqueos.queNoPuedeVer(any())).thenReturn(NADIE);

        service = new RatingService(
            ratingRepository, mock(BurgerJointRepository.class), mock(UserRepository.class),
            mock(FotosDeResenias.class), followRepository, bloqueos
        );
    }

    // ---- la lista ----

    /**
     * La tercera pantalla donde aparece gente, después del feed y del buscador.
     *
     * Antes era la única que no miraba los bloqueos: para volver a cruzarte con alguien
     * que bloqueaste alcanzaba con abrir una hamburguesería que los dos hubieran
     * visitado.
     */
    @Test
    void laListaNoTraeAQuienBloqueaste() {
        when(bloqueos.queNoPuedeVer(YO)).thenReturn(List.of(8L));

        service.list(LOCAL, YO, PRIMERA_PAGINA);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Long>> ocultos = ArgumentCaptor.forClass(List.class);
        verify(ratingRepository).deUnLocalSalvo(eq(LOCAL), ocultos.capture(), eq(PRIMERA_PAGINA));
        assertThat(ocultos.getValue()).containsExactly(8L);
    }

    /** Sin sesión no hay bloqueos, pero la lista tiene que salir igual. */
    @Test
    void sinSesionSeVenTodas() {
        Page<Rating> unaPagina = new PageImpl<>(List.of(unaResenia(3L, "otro", 4)));
        when(ratingRepository.deUnLocalSalvo(LOCAL, NADIE, PRIMERA_PAGINA)).thenReturn(unaPagina);

        Page<RatingResponse> lista = service.list(LOCAL, null, PRIMERA_PAGINA);

        assertThat(lista.getContent()).extracting(RatingResponse::username).containsExactly("otro");
    }

    // ---- el resumen de arriba ----

    @Test
    void lasCincoNotasEstanAunqueLaBaseDevuelvaDos() {
        when(ratingRepository.distribucionDeNotas(LOCAL)).thenReturn(List.of(
            new NotaYCuantasDto(5, 2L),
            new NotaYCuantasDto(3, 1L)
        ));

        ResumenDeReseniasDto resumen = service.resumen(LOCAL, null);

        assertThat(resumen.distribucion()).hasSize(5);
        assertThat(resumen.distribucion()).extracting(NotaYCuantasDto::nota)
            .containsExactly(1, 2, 3, 4, 5);
        assertThat(resumen.distribucion()).extracting(NotaYCuantasDto::cuantas)
            .containsExactly(0L, 0L, 1L, 0L, 2L);
    }

    @Test
    void unLocalSinReseniasDaCincoCerosYNoUnaListaVacia() {
        ResumenDeReseniasDto resumen = service.resumen(LOCAL, null);

        assertThat(resumen.distribucion()).hasSize(5);
        assertThat(resumen.distribucion()).extracting(NotaYCuantasDto::cuantas).containsOnly(0L);
    }

    /**
     * La distribución cuenta a todos, también a quien bloqueaste.
     *
     * El puntaje de una hamburguesería es un hecho del lugar y no de quién lo mira: si
     * dependiera de a quién bloqueaste, dos personas verían promedios distintos del
     * mismo local y ninguna sabría por qué. Se esconde a la persona, no se reescribe la
     * nota.
     */
    @Test
    void laDistribucionNoMiraLosBloqueos() {
        when(bloqueos.queNoPuedeVer(YO)).thenReturn(List.of(8L));

        service.resumen(LOCAL, YO);

        verify(ratingRepository).distribucionDeNotas(LOCAL);
    }

    /** Un local se puede mirar sin cuenta: la parte social viene vacía, no falla. */
    @Test
    void sinSesionNoSePreguntaAQuienSigue() {
        ResumenDeReseniasDto resumen = service.resumen(LOCAL, null);

        assertThat(resumen.deQuienesSigo()).isEmpty();
        verify(followRepository, never()).idsQueSigue(any());
    }

    @Test
    void siNoSigoANadieNoSeVaABuscarNingunaResenia() {
        ResumenDeReseniasDto resumen = service.resumen(LOCAL, YO);

        assertThat(resumen.deQuienesSigo()).isEmpty();
        verify(ratingRepository, never()).deAutoresEn(any(), any());
    }

    /**
     * Seguir a alguien no alcanza para verlo si hay un bloqueo de por medio.
     *
     * Hoy bloquear borra el seguir en las dos direcciones, así que esta situación no
     * debería poder existir; la prueba está para que el día que alguien toque esa otra
     * regla, la consecuencia no sea que un bloqueado reaparezca acá.
     */
    @Test
    void unBloqueadoNoAparecePorMasQueLoSigas() {
        when(followRepository.idsQueSigue(YO)).thenReturn(List.of(7L, 8L));
        when(bloqueos.queNoPuedeVer(YO)).thenReturn(List.of(8L));
        when(ratingRepository.deAutoresEn(any(), any())).thenReturn(List.of());

        service.resumen(LOCAL, YO);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Long>> autores = ArgumentCaptor.forClass(List.class);
        verify(ratingRepository).deAutoresEn(any(), autores.capture());
        assertThat(autores.getValue()).containsExactly(7L);
    }

    @Test
    void lasReseniasDeLosQueSigoLleganConSusFotos() {
        Instant cuando = Instant.now();
        Rating deUnAmigo = Rating.builder().id(9L).score(5).comment("una masa").createdAt(cuando)
            .user(User.builder().id(7L).username("amigo").hamburguesa("20131").build())
            .burgerJoint(BurgerJoint.builder().id(LOCAL).name("Un local").build())
            .build();
        deUnAmigo.ponerFotos(List.of("/api/rating-photos/x.jpg", "/api/rating-photos/y.jpg"));
        when(followRepository.idsQueSigue(YO)).thenReturn(List.of(7L));
        when(ratingRepository.deAutoresEn(LOCAL, List.of(7L))).thenReturn(List.of(deUnAmigo));

        ResumenDeReseniasDto resumen = service.resumen(LOCAL, YO);

        assertThat(resumen.deQuienesSigo()).containsExactly(new RatingResponse(
            9L, 7L, "amigo", "20131", 5, "una masa",
            List.of("/api/rating-photos/x.jpg", "/api/rating-photos/y.jpg"), cuando));
    }

    // ---- de qué hablan las reseñas ----

    /**
     * Que lo que cuenta el resumen llegue hasta la pantalla.
     *
     * Qué cuenta como mención se prueba en TemasDeLasReseniasTest, que no necesita ni
     * base ni Spring. Lo único que falta comprobar acá es que estas reseñas son las que
     * se le pasan: con la consulta mal escrita el resumen quedaría siempre vacío, y
     * vacío es exactamente lo que se ve en un local sin reseñas.
     */
    @Test
    void elResumenCuentaDeQueHablanLasResenias() {
        when(ratingRepository.notasYComentarios(LOCAL)).thenReturn(List.of(
            new NotaYComentarioDto(5, "La carne, espectacular"),
            new NotaYComentarioDto(4, "Muy buena la carne"),
            new NotaYComentarioDto(2, "La carne venía cruda")));

        ResumenDeReseniasDto resumen = service.resumen(LOCAL, null);

        assertThat(resumen.temas()).hasSize(1);
        assertThat(resumen.temas().get(0).tema()).isEqualTo("La carne");
        assertThat(resumen.temas().get(0).menciones()).isEqualTo(3);
        assertThat(resumen.temas().get(0).aFavor()).isEqualTo(2);
    }

    /** Un local sin reseñas escritas no muestra resumen, y eso no es un error. */
    @Test
    void sinReseniasEscritasElResumenViajaVacio() {
        assertThat(service.resumen(LOCAL, null).temas()).isEmpty();
    }

    private Rating unaResenia(Long id, String username, int nota) {
        return Rating.builder()
            .id(id)
            .user(User.builder().id(id).username(username).email(username + "@example.com").build())
            .burgerJoint(BurgerJoint.builder().id(LOCAL).name("Un local").build())
            .score(nota)
            .createdAt(Instant.now())
            .build();
    }
}
