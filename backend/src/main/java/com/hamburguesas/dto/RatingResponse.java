package com.hamburguesas.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record RatingResponse(
    Long id,
    Long userId,
    String username,
    /** La hamburguesa del avatar de quien la escribió, o null. */
    String hamburguesa,
    /** La XP de quien la escribió, para dibujar su nivel (#310). */
    int xp,
    Integer score,
    String comment,
    /** Las fotos que sacó quien la escribió, en orden: la primera es la portada. */
    List<String> fotos,
    Instant createdAt,
    /** El día de la visita, o null en las de antes: ahí se muestra createdAt (#336). */
    LocalDate visitDate,
    /** Las notas opcionales de local, atención y precio; nulas si no las puso (#336). */
    Integer scoreLocal,
    Integer scoreAtencion,
    Integer scorePrecio,
    /** Las reacciones que le pusieron los demás, y la de quien mira (#186). */
    ReaccionesDto reacciones
) {}
