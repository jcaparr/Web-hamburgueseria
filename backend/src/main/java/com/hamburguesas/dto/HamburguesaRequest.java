package com.hamburguesas.dto;

import jakarta.validation.constraints.Pattern;

/**
 * La hamburguesa que alguien eligió para su avatar.
 *
 * Son cinco cifras, una por cosa que se elige y en este orden: el fondo (0 a 4), el pan
 * (0 a 2), el queso (0 a 2), lo verde (0 a 3) y cuántas carnes (1 a 4). Con algún premio
 * del nivel (#313) llevan tres más al final, de 0 o 1: huevo, bacon y fondo de rayos. Qué es cada
 * número lo sabe solo el navegador, que es el que dibuja; acá alcanza con que sean
 * cinco u ocho cifras dentro de su rango, para que lo que se guarde siempre se pueda
 * dibujar. Que el nivel alcance para los premios lo mira {@code ProfileService}.
 *
 * @param receta las cinco cifras, o null para volver a la que sale del nombre
 */
public record HamburguesaRequest(
    @Pattern(regexp = FORMATO, message = "Esa hamburguesa no se puede armar")
    String receta
) {
    public static final String FORMATO = "[0-4][0-2][0-2][0-3][1-4]([01]{3})?";
}
