package com.hamburguesas.dto;

import java.time.Instant;

public record ReseniaDePerfilDto(
    Long id,
    Long burgerJointId,
    String burgerJointName,
    String photoUrl,
    Integer score,
    String comment,
    Instant createdAt
) {}
