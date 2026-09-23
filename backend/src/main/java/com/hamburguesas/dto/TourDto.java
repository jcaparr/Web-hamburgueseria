package com.hamburguesas.dto;

import java.util.List;

/** Un recorrido armado: por dónde pasar, en qué orden y cuánto se camina. */
public record TourDto(
    List<TourStopDto> paradas,
    /** Todo el recorrido, estimado a pie. */
    double kilometros,
    int minutos,
    /** Entre cuántas hamburgueserías se eligió, para saber si el filtro quedó chico. */
    int candidatos,
    /** Qué no se pudo cumplir del pedido, o nulo si salió entero. */
    String aviso
) {}
