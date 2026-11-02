package com.hamburguesas.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * El nombre de usuario que quiere tener. Las reglas de forma y de disponibilidad las
 * resuelve el servidor ({@code Usernames} y {@code UsernameService}), no esta anotación.
 */
public record CambioDeNombreRequest(@NotBlank String username) {}
