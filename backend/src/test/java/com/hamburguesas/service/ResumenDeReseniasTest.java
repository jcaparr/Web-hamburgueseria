package com.hamburguesas.service;

import com.hamburguesas.dto.NotaYCuantasDto;
import com.hamburguesas.dto.RatingResponse;
import com.hamburguesas.dto.ResumenDeReseniasDto;
import com.hamburguesas.fotos.FotosDeResenias;
import com.hamburguesas.repository.BurgerJointRepository;
import com.hamburguesas.repository.FollowRepository;
import com.hamburguesas.repository.RatingRepository;
import com.hamburguesas.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Lo que se muestra arriba de la lista de reseñas de un local.
 *
 * Las dos cosas que se prueban acá son las que no se ven mirando la consulta: que las
 * cinco barras estén aunque la base devuelva dos, y que seguir a alguien no alcance
 * para verlo si hay un bloqueo de por medio.
 */
class ResumenDeReseniasTest {

    private static final Long YO = 1L;
    private static final Long LOCAL = 5L;

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
        when(followRepository.idsQueSigue(anyLong())).thenReturn(List.of());
        when(bloqueos.queNoPuedeVer(anyLong())).thenReturn(List.of(-1L));

        service = new RatingService(
            ratingRepository, mock(BurgerJointRepository.class), mock(UserRepository.class),
            mock(FotosDeResenias.class), followRepository, bloqueos
        );
    }

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
        assertThat(resumen.total()).isEqualTo(3);
    }

    @Test
    void unLocalSinReseniasDaCincoCerosYNoUnaListaVacia() {
        ResumenDeReseniasDto resumen = service.resumen(LOCAL, null);

        assertThat(resumen.distribucion()).hasSize(5);
        assertThat(resumen.distribucion()).extracting(NotaYCuantasDto::cuantas)
            .containsOnly(0L);
        assertThat(resumen.total()).isZero();
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
    void lasReseniasDeLosQueSigoLleganTalCual() {
        RatingResponse deUnAmigo = new RatingResponse(
            9L, 7L, "amigo", 5, "una masa", "/api/rating-photos/x.jpg", Instant.now());
        when(followRepository.idsQueSigue(YO)).thenReturn(List.of(7L));
        when(ratingRepository.deAutoresEn(LOCAL, List.of(7L))).thenReturn(List.of(deUnAmigo));

        ResumenDeReseniasDto resumen = service.resumen(LOCAL, YO);

        assertThat(resumen.deQuienesSigo()).containsExactly(deUnAmigo);
    }
}
