package com.hamburguesas.dto;

public record HamburgueseriaDto(
    Long id,
    String nombre,
    String direccion,
    String zona,
    String fotoUrl,
    Double latitud,
    Double longitud,
    Double promedio,
    Long cantidadCalificaciones,
    boolean enListaDeseados
) {}
