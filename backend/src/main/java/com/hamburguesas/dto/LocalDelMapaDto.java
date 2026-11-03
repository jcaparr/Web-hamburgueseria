package com.hamburguesas.dto;

import java.util.List;

/**
 * Lo justo de un local para ponerlo en el mapa (#395): un punto con su foto y su nota, y
 * la tarjeta que se abre al tocarlo. Nada de la ficha entera.
 *
 * @param placeId    el de Google, que es público: sirve para que "Cómo llegar" abra la ficha del local en Maps
 * @param miniatura  la foto chica del punto; nula si el local no tiene foto
 * @param averageScore nula si todavía nadie lo reseñó: el punto lleva solo la foto
 * @param franjas    el horario de la semana, para decir "Abierto ahora" en el navegador (ver
 *                   HorarioDto: cómo se lee abierto o cerrado depende de la hora de quien mira)
 */
public record LocalDelMapaDto(
    Long id,
    String placeId,
    String slug,
    String name,
    String area,
    String miniatura,
    double latitude,
    double longitude,
    Double averageScore,
    long ratingsCount,
    List<HorarioDto.Franja> franjas
) {}
