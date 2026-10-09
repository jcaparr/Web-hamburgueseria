package com.hamburguesas.dto;

/**
 * Una cadena en el buscador de Explorar: una tarjeta por cadena y no una por sucursal (#206).
 *
 * @param marca      la clave, "mcdonalds": es la que va en la dirección de su página
 * @param nombre     cómo se escribe, "McDonald's"
 * @param sucursales cuántas hay en los barrios pedidos, o en total si no se pidió ninguno
 * @param fotoUrl    la portada más repetida entre sus sucursales, que casi siempre es la
 *                   misma prestada de una a otra; nula si ninguna tiene
 */
public record CadenaDto(String marca, String nombre, long sucursales, String fotoUrl) {}
