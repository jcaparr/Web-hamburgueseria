package com.hamburguesas.dto;

/**
 * Cuántas reseñas tiene una nota en un local.
 *
 * Un promedio de 4.0 puede ser todo 4, o mitad 5 y mitad 3, y no son la misma
 * hamburguesería: la primera es pareja y la segunda divide opiniones. El promedio solo
 * no distingue esos dos casos, y esto sí.
 */
public record NotaYCuantasDto(Integer nota, Long cuantas) {}
