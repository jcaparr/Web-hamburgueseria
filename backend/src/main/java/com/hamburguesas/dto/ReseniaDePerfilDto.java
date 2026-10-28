package com.hamburguesas.dto;

import java.time.Instant;
import java.time.LocalDate;

public record ReseniaDePerfilDto(
    Long id,
    Long burgerJointId,
    String slug,
    String burgerJointName,
    String photoUrl,
    Integer score,
    String comment,
    Instant createdAt,
    /** El día que dijo haber ido, o null en las de antes: ahí vale createdAt (#368). */
    LocalDate visitDate
) {}
