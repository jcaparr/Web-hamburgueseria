package com.hamburguesas.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VerifyEmailRequest(
    @NotBlank @Email @Size(max = 180) String email,
    @NotBlank @Pattern(regexp = "\\d{6}", message = "debe ser un código de 6 dígitos") String code
) {}
