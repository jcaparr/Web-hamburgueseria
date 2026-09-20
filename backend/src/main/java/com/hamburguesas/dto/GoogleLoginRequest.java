package com.hamburguesas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** The ID token Google hands to the browser. */
public record GoogleLoginRequest(
    @NotBlank @Size(max = 4096) String credential
) {}
