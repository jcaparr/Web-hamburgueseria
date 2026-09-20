package com.hamburguesas.dto;

/**
 * Who the user is. No token: the session lives in cookies the browser keeps away
 * from JavaScript.
 */
public record AuthResponse(
    Long userId,
    String name,
    String email
) {}
