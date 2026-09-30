package com.hamburguesas.dto;

import java.util.List;

/**
 * Lo que se muestra arriba de la lista de reseñas de un local.
 *
 * Las dos cosas viajan juntas porque se piden juntas y se muestran en la misma
 * pantalla: separarlas serían dos idas al servidor para dibujar una sola vista.
 */
public record ResumenDeReseniasDto(
    /**
     * Cuántas de cada nota, del 1 al 5, siempre las cinco.
     *
     * Las notas que nadie puso vienen en cero y no ausentes: quien dibuja las barras
     * necesita las cinco para que la escala no se mueva de local en local.
     */
    List<NotaYCuantasDto> distribucion,
    long total,
    /**
     * Las reseñas de la gente que seguís sobre este local, de la más nueva a la más
     * vieja. Vacía si no seguís a nadie que haya venido, o si no hay sesión.
     */
    List<RatingResponse> deQuienesSigo
) {}
