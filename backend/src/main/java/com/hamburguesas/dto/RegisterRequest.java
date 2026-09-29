package com.hamburguesas.dto;

import com.hamburguesas.auth.Usernames;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank @Size(max = 120) String name,
    // Se acepta con mayúsculas y el servicio lo baja: rechazar "Juan" por la mayúscula
    // sería incomprensible para quien lo escribió.
    @NotBlank @Pattern(regexp = Usernames.FORMA,
        message = "Entre 3 y 20 caracteres, solo letras, números y guión bajo")
    String username,
    @NotBlank @Email @Size(max = 180) String email,
    @NotBlank @Size(min = 8, max = 72) String password
) {}
