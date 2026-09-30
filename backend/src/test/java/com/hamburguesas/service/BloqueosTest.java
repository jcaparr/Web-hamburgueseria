package com.hamburguesas.service;

import com.hamburguesas.repository.BlockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * La lista de gente que no tiene que aparecer.
 *
 * Lo importante de acá es que nunca vuelva vacía: las consultas filtran con "not in",
 * y un "not in ()" no es SQL válido, así que una lista vacía no escondería a nadie
 * sino que rompería la pantalla entera.
 */
class BloqueosTest {

    private static final Long YO = 1L;

    private BlockRepository blockRepository;
    private Bloqueos bloqueos;

    @BeforeEach
    void setUp() {
        blockRepository = mock(BlockRepository.class);
        when(blockRepository.idsQueNoPuedeVer(anyLong())).thenReturn(List.of());

        bloqueos = new Bloqueos(blockRepository);
    }

    @Test
    void traeConQuienesHayUnBloqueoDePorMedio() {
        when(blockRepository.idsQueNoPuedeVer(YO)).thenReturn(List.of(5L, 8L));

        assertThat(bloqueos.queNoPuedeVer(YO)).containsExactlyInAnyOrder(5L, 8L);
    }

    /** Sin nadie bloqueado igual devuelve algo, porque un "not in ()" no es SQL. */
    @Test
    void sinNadieBloqueadoDevuelveUnIdImposible() {
        assertThat(bloqueos.queNoPuedeVer(YO)).containsExactly(-1L);
    }

    /** Sin sesión no hay a quién esconder, y tampoco hay nada que preguntarle a la base. */
    @Test
    void sinSesionNoSeConsultaNada() {
        assertThat(bloqueos.queNoPuedeVer(null)).containsExactly(-1L);

        verify(blockRepository, never()).idsQueNoPuedeVer(anyLong());
    }

    /** Para el buscador, uno mismo va en la misma bolsa: tampoco tiene que salir. */
    @Test
    void paraElBuscadorSeAgregaUnoMismo() {
        when(blockRepository.idsQueNoPuedeVer(YO)).thenReturn(List.of(5L));

        assertThat(bloqueos.queNoPuedeVerNiASiMismo(YO)).containsExactlyInAnyOrder(5L, YO);
    }

    @Test
    void yEsoAlcanzaParaQueNuncaVengaVacia() {
        assertThat(bloqueos.queNoPuedeVerNiASiMismo(YO)).containsExactly(YO);
    }

    /**
     * La lista que devuelve la base puede ser inmutable, y a esta se le agrega algo.
     *
     * Es el error que rompería solo en producción: List.of() no deja agregar, y el
     * mock de un test podría devolver una lista que sí.
     */
    @Test
    void agregarUnoMismoNoRompeSiLaBaseDevolvioAlgoInmutable() {
        when(blockRepository.idsQueNoPuedeVer(YO)).thenReturn(List.of(5L, 8L));

        assertThat(bloqueos.queNoPuedeVerNiASiMismo(YO)).hasSize(3);
    }
}
