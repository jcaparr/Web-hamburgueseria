package com.hamburguesas.dto;

import com.hamburguesas.model.TipoDeReaccion;

import java.util.List;

/**
 * Las reacciones de una reseña: cuántas hay de cada una, y cuál le puso quien mira.
 *
 * @param cuantas solo las que alguien usó, de la más usada a la menos
 * @param mia     la de quien mira, o null si no reaccionó o no tiene sesión
 */
public record ReaccionesDto(List<CuantasReaccionesDto> cuantas, TipoDeReaccion mia) {

    public static final ReaccionesDto NINGUNA = new ReaccionesDto(List.of(), null);
}
