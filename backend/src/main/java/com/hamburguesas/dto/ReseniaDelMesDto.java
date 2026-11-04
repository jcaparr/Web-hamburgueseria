package com.hamburguesas.dto;

import java.time.Instant;

/** Una reseña de un mes, con lo justo del local para armar el resumen (#308). */
public record ReseniaDelMesDto(
    Long localId, String slug, String nombre, String barrio, int puntaje, Instant cuando, Long id, String comentario
) {}
