package com.hamburguesas.dto;

/**
 * Cuántas reseñas tiene alguien, para varias personas de una.
 *
 * Existe para que una pantalla de resultados se resuelva con una consulta y no con
 * una por fila.
 */
public record ReseniasPorUsuarioDto(Long userId, long total) {}
