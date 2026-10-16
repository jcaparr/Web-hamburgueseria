package com.hamburguesas.dto;

import java.util.List;

/**
 * El perfil de otro, como lo ve cualquiera.
 *
 * Tampoco lleva el email, ni con qué método entra, ni cuándo se creó la cuenta. Se
 * arma campo por campo en vez de devolver el usuario y confiar en que Jackson no
 * serialice de más: lo que no está en este record no puede salir por acá.
 *
 * Las guardadas y los recorridos no vienen acá: marcar una hamburguesería para ir algún
 * día es una intención, no una opinión publicada. Se piden aparte, y solo si su dueño
 * eligió mostrarlos (#264); este record dice si se pueden pedir.
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
    List<ReseniaDePerfilDto> ultimasResenias,
    /** Si quien mira puede ver sus guardadas y sus recorridos (#264). Las propias, siempre. */
    boolean seVenSusListas
) {}
