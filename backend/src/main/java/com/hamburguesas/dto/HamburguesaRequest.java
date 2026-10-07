package com.hamburguesas.dto;

import jakarta.validation.constraints.Pattern;

/**
 * La hamburguesa que alguien eligió para su avatar.
 *
 * Son cinco cifras, una por cosa que se elige y en este orden: el fondo (0 a 4), el pan
 * (0 a 2), el queso (0 a 2), lo verde (0 a 3) y cuántas carnes (1 a 3). Qué es cada
 * número lo sabe solo el navegador, que es el que dibuja; acá alcanza con que sean
 * cinco cifras dentro de su rango, para que lo que se guarde siempre se pueda dibujar.
 *
 * @param receta las cinco cifras, o null para volver a la que sale del nombre
 */
public record HamburguesaRequest(
    @Pattern(regexp = FORMATO, message = "Esa hamburguesa no se puede armar")
    String receta
) {
    public static final String FORMATO = "[0-4][0-2][0-2][0-3][1-3]";
}
