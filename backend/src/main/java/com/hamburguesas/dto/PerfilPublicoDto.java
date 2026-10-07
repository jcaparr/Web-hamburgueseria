package com.hamburguesas.dto;

import java.util.List;

/**
 * El perfil de otro, como lo ve cualquiera.
 *
 * Tampoco lleva el email, ni con qué método entra, ni cuándo se creó la cuenta. Se
 * arma campo por campo en vez de devolver el usuario y confiar en que Jackson no
 * serialice de más: lo que no está en este record no puede salir por acá.
 *
 * Los favoritos quedan afuera a propósito. Marcar una hamburguesería para ir algún
 * día es una intención, no una opinión publicada, y no hay razón para que la vea
 * alguien que no sea quien la guardó.
 */
public record PerfilPublicoDto(
    Long userId,
    String username,
    String hamburguesa,
    long resenias,
    Double promedio,
    long seguidores,
    long siguiendo,
    /** Si quien mira ya lo sigue. Falso cuando mira su propio perfil. */
    boolean loSigo,
    boolean soyYo,
    List<ReseniaDePerfilDto> ultimasResenias
) {}
