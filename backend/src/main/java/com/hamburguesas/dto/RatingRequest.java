package com.hamburguesas.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record RatingRequest(
    @NotNull @Min(1) @Max(5) Integer score,
    @Size(max = 1000) String comment,
    /** El día de la visita, "2026-10-12". Opcional; el servicio valida el rango (#336). */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate visitDate,
    /** Las notas opcionales de local, atención y precio (#336). */
    @Min(1) @Max(5) Integer scoreLocal,
    @Min(1) @Max(5) Integer scoreAtencion,
    @Min(1) @Max(5) Integer scorePrecio
) {
    /** Solo la nota general y el comentario, que es lo que mandaba la app de antes. */
    public RatingRequest(Integer score, String comment) {
        this(score, comment, null, null, null, null);
    }
}
