package com.hamburguesas.dto;

import com.hamburguesas.auth.EntraEnBcrypt;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
    @NotBlank @Email @Size(max = 180) String email,
    @NotBlank @Pattern(regexp = "\\d{6}", message = "debe ser un código de 6 dígitos") String code,
    // Lo largo lo mide en bytes @EntraEnBcrypt, no en letras: ver ahí por qué.
    @NotBlank @Size(min = 8, message = "La contraseña tiene que tener al menos 8 caracteres") @EntraEnBcrypt
    String newPassword
) {}
