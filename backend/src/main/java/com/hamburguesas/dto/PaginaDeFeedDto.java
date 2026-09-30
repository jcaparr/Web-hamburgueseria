package com.hamburguesas.dto;

import java.util.List;

/**
 * Un tramo del feed y por dónde seguir.
 *
 * El cursor dice desde qué reseña continuar, en vez de cuántas saltear. Con un número
 * de página, una reseña nueva que entra arriba corre todo hacia abajo y al pedir la
 * página siguiente se vuelve a ver la última de la anterior.
 *
 * Viene en null cuando no hay más.
 */
public record PaginaDeFeedDto(
    List<ItemDeFeedDto> items,
    String siguiente
) {}
