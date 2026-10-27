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
 * @param localSlug  solo en una reacción: la dirección linda de ese local (#305), o null si aún no tiene
 * @param localNombre solo en una reacción
 */
public record NotificacionDto(
    Tipo tipo,
    Instant cuando,
    boolean nueva,
    Long userId,
    String username,
    String hamburguesa,
    int xp,
    boolean loSigo,
    TipoDeReaccion reaccion,
    Long localId,
    String localSlug,
    String localNombre,
    /** Solo en un logro (#320): su código, o RETROACTIVOS en el aviso único de la carga inicial. */
    String logro,
    /** Solo en el aviso de retroactivos: cuántos logros eran. */
    Long cantidad
) {

    public enum Tipo { SEGUIMIENTO, REACCION, LOGRO }
}
