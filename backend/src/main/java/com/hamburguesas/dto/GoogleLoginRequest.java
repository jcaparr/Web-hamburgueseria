package com.hamburguesas.dto;

import com.hamburguesas.auth.Usernames;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * The ID token Google hands to the browser.
 *
 * El nombre de usuario viaja vacío salvo en un caso: cuando es la primera vez y el
 * servidor ya contestó que hace falta elegir uno. La cuenta se crea recién en esa
 * segunda vuelta, para que nunca exista a medio hacer.
 */
public record GoogleLoginRequest(
    @NotBlank @Size(max = 4096) String credential,
    @Pattern(regexp = Usernames.FORMA,
        message = "Entre 3 y 20 caracteres, solo letras, números y guión bajo")
    String username
) {}
