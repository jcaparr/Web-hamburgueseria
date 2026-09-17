package com.hamburguesas.dto;

public record AuthResponse(
    String token,
    Long usuarioId,
    String nombre,
    String email
) {}
