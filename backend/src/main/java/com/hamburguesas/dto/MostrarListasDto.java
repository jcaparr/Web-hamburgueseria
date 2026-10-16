package com.hamburguesas.dto;

import jakarta.validation.constraints.NotNull;

/** Si muestra sus guardadas y sus recorridos en su perfil público (#264). */
public record MostrarListasDto(@NotNull Boolean muestra) {}
