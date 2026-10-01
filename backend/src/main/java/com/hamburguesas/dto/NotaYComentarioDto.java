package com.hamburguesas.dto;

/**
 * Lo que una reseña aporta al resumen: qué nota puso y qué escribió.
 *
 * No trae quién la escribió a propósito. El resumen cuenta de qué habla la gente, no
 * quién dijo qué, y lo que no sale del repositorio no se puede filtrar mal más adelante.
 */
public record NotaYComentarioDto(Integer nota, String comentario) {}
