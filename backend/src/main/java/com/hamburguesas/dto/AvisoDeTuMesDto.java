package com.hamburguesas.dto;

import java.time.Instant;

/**
 * Si hay un resumen de "Tu mes en Burgómetro" para avisar (#405).
 *
 * @param hayResumen si hubo al menos una reseña en el período que se resume
 * @param mes        el mes del resumen, "2026-10"; en el anual, "2026-12"
 * @param anual      si es el recuento del año entero
 * @param visto      si la persona ya vio el pop up de este resumen
 * @param desde      cuándo salió: la medianoche de Buenos Aires del primer día del mes en curso
 */
public record AvisoDeTuMesDto(boolean hayResumen, String mes, boolean anual, boolean visto, Instant desde) {}
