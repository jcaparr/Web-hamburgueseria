package com.hamburguesas.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Lo que manda el formulario "Sugerir un local" (#257). */
public record SugerenciaDeLocalRequest(
    @NotBlank @Size(max = 120) String nombre,
    @NotBlank @Size(max = 300) String direccion,
    @NotBlank @Email @Size(max = 254) String email,
    @Size(max = 500) String comentario
) {}
