package com.hamburguesas.dto;

/**
 * Who the user is. No token: the session lives in cookies the browser keeps away
 * from JavaScript.
 */
public record AuthResponse(
    Long userId,
    String username,
    /** La hamburguesa del avatar, en cinco cifras, o null si no eligió ninguna. */
    String hamburguesa,
    String email
) {}
