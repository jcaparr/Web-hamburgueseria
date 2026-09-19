package com.hamburguesas.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Used by "resend code" and "forgot password", which take nothing but an address. */
public record EmailOnlyRequest(
    @NotBlank @Email @Size(max = 180) String email
) {}
