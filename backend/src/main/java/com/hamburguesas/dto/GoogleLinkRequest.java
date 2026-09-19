package com.hamburguesas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Confirms linking a Google account to an account that already exists with the same
 * email. The credential is sent again so the link is proved, not just asserted by
 * whoever holds the code.
 */
public record GoogleLinkRequest(
    @NotBlank @Size(max = 4096) String credential,
    @NotBlank @Pattern(regexp = "\\d{6}", message = "debe ser un código de 6 dígitos") String code
) {}
