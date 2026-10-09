package com.hamburguesas.dto;

import com.hamburguesas.model.TipoDeReaccion;

import java.time.Instant;

/**
 * Un aviso del buzón (#210): alguien te siguió, o alguien reaccionó a una reseña tuya.
 *
 * Un solo tipo con campos que a veces van vacíos, y no dos: el buzón los muestra
 * mezclados y ordenados por fecha, y la pantalla decide qué dibujar según el tipo.
 *
 * @param nueva      si llegó después de la última vez que abrió el buzón
 * @param loSigo     solo en un seguimiento: si quien mira ya lo sigue, para el botón de
 *                   seguir de vuelta
 * @param reaccion   solo en una reacción: cuál
 * @param localId    solo en una reacción: el local de la reseña, para llevar a su ficha
 * @param localNombre solo en una reacción
 */
public record NotificacionDto(
    Tipo tipo,
    Instant cuando,
    boolean nueva,
    Long userId,
    String username,
    String hamburguesa,
    boolean loSigo,
    TipoDeReaccion reaccion,
    Long localId,
    String localNombre
) {

    public enum Tipo { SEGUIMIENTO, REACCION }
}
