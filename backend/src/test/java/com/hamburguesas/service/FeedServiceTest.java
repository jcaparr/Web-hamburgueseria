package com.hamburguesas.service;

import com.hamburguesas.dto.ItemDeFeedDto;
import com.hamburguesas.model.FuenteDelFeed;
import com.hamburguesas.repository.FollowRepository;
import com.hamburguesas.repository.RatingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** El feed: qué se trae, hasta dónde, y por dónde sigue. */
class FeedServiceTest {

    private static final Long YO = 1L;
    private static final Instant CUANDO = Instant.parse("2026-09-20T15:00:00Z");

    private RatingRepository ratingRepository;
    private FollowRepository followRepository;
    private Bloqueos bloqueos;
    private FeedService service;

    @BeforeEach
    void setUp() {
        ratingRepository = mock(RatingRepository.class);
        followRepository = mock(FollowRepository.class);

        when(ratingRepository.feedDeTodos(any(), any(), anyCollection(), any()))
            .thenReturn(List.of());
        when(ratingRepository.feedDe(anyCollection(), any(), any(), anyCollection(), any()))
            .thenReturn(List.of());
        when(followRepository.idsQueSigue(anyLong())).thenReturn(List.of());

        bloqueos = mock(Bloqueos.class);
        when(bloqueos.queNoPuedeVer(any())).thenReturn(List.of(-1L));

        service = new FeedService(ratingRepository, followRepository, bloqueos);
    }

    private ItemDeFeedDto resenia(long id, Instant cuando) {
        return new ItemDeFeedDto(id, 9L, "juanca", 5L, "Un local", null, "Palermo",
            4, "buena", cuando, false);
    }

    /** Tantas como para que sobre una y haya página siguiente. */
    private List<ItemDeFeedDto> cuantas(int cantidad) {
        return IntStream.range(0, cantidad)
            .mapToObj(i -> resenia(100 - i, CUANDO.minusSeconds(i)))
            .toList();
    }

    private Instant fechaPedida() {
        ArgumentCaptor<Instant> fecha = ArgumentCaptor.forClass(Instant.class);
        verify(ratingRepository).feedDeTodos(
            fecha.capture(), any(), anyCollection(), any(Pageable.class));
        return fecha.getValue();
    }

    private Long idPedido() {
        ArgumentCaptor<Long> id = ArgumentCaptor.forClass(Long.class);
        verify(ratingRepository).feedDeTodos(
            any(), id.capture(), anyCollection(), any(Pageable.class));
        return id.getValue();
    }

    @Test
    void sinCursorEmpiezaPorElPrincipio() {
        service.ver(FuenteDelFeed.TODOS, null, YO);

        assertThat(fechaPedida()).isAfter(Instant.now());
        assertThat(idPedido()).isEqualTo(Long.MAX_VALUE);
    }

    /**
     * Con veinte justas no hay página siguiente.
     *
     * Se piden veintiuna para saberlo sin contar el total: si vuelven veinte, se acabó.
     */
    @Test
    void siNoSobraNingunaNoHayCursorParaSeguir() {
        when(ratingRepository.feedDeTodos(any(), any(), anyCollection(), any())).thenReturn(cuantas(20));

        var pagina = service.ver(FuenteDelFeed.TODOS, null, YO);

        assertThat(pagina.items()).hasSize(20);
        assertThat(pagina.siguiente()).isNull();
    }

    /** Y la de más no se muestra: sirve para saber que hay, no para llenar la pantalla. */
    @Test
    void laQueSobraNoSeMuestraPeroAvisaQueHayMas() {
        when(ratingRepository.feedDeTodos(any(), any(), anyCollection(), any())).thenReturn(cuantas(21));

        var pagina = service.ver(FuenteDelFeed.TODOS, null, YO);

        assertThat(pagina.items()).hasSize(20);
        assertThat(pagina.siguiente()).isNotNull();
    }

    /** El cursor apunta a la última mostrada, no a la que sobró. */
    @Test
    void elCursorApuntaALaUltimaQueSeMostro() {
        when(ratingRepository.feedDeTodos(any(), any(), anyCollection(), any())).thenReturn(cuantas(21));

        var pagina = service.ver(FuenteDelFeed.TODOS, null, YO);
        ItemDeFeedDto ultima = pagina.items().get(19);

        assertThat(pagina.siguiente())
            .isEqualTo(ultima.createdAt().toEpochMilli() + "_" + ultima.ratingId());
    }

    @Test
    void elCursorSeVuelveElCorteDeLaProximaConsulta() {
        service.ver(FuenteDelFeed.TODOS, CUANDO.toEpochMilli() + "_77", YO);

        assertThat(fechaPedida()).isEqualTo(CUANDO);
        assertThat(idPedido()).isEqualTo(77L);
    }

    /**
     * Un cursor roto muestra el feed desde arriba en vez de fallar.
     *
     * Quien llega con uno inventado o viejo ve algo razonable; devolverle un error
     * dejaría la pantalla vacía por un parámetro que él no escribió.
     */
    @Test
    void unCursorQueNoSeEntiendeEmpiezaDeNuevo() {
        service.ver(FuenteDelFeed.TODOS, "cualquier-cosa", YO);

        assertThat(fechaPedida()).isAfter(Instant.now());
        assertThat(idPedido()).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    void unCursorConNumerosInvalidosTambien() {
        service.ver(FuenteDelFeed.TODOS, "ayer_primera", YO);

        assertThat(fechaPedida()).isAfter(Instant.now());
    }

    @Test
    void siguiendoTraeSoloLasDeQuienesSigue() {
        when(followRepository.idsQueSigue(YO)).thenReturn(List.of(7L, 8L));

        service.ver(FuenteDelFeed.SIGUIENDO, null, YO);

        verify(ratingRepository).feedDe(
            org.mockito.ArgumentMatchers.eq(List.of(7L, 8L)), any(), any(), anyCollection(),
            any(Pageable.class));
    }

    /**
     * Sin seguir a nadie, la pestaña está vacía y no se le pregunta a la base.
     *
     * Un "in ()" vacío no tiene sentido, y la respuesta ya se sabe sin consultar.
     */
    @Test
    void sinSeguirANadieNoSeConsultaNada() {
        var pagina = service.ver(FuenteDelFeed.SIGUIENDO, null, YO);

        assertThat(pagina.items()).isEmpty();
        assertThat(pagina.siguiente()).isNull();
        verify(ratingRepository, never())
            .feedDe(anyCollection(), any(), any(), anyCollection(), any());
    }

    /** La pestaña de todos no mira a quién seguís. */
    @Test
    void laPestaniaDeTodosNoMiraLosSeguidos() {
        service.ver(FuenteDelFeed.TODOS, null, YO);

        verify(followRepository, never()).idsQueSigue(any());
    }
}
