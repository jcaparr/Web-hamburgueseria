package com.hamburguesas.dto;

import java.time.LocalDate;

/**
 * Si puede cambiar su nombre de usuario ya o desde cuándo (#380).
 *
 * @param puedeCambiarDesde el primer día (de Buenos Aires) en que puede, o null si ya puede
 * @param texto             ese día en palabras ("12 de diciembre"), o null si ya puede
 */
public record EstadoDelNombreDto(LocalDate puedeCambiarDesde, String texto) {}
