package com.hamburguesas.dto;

/**
 * El promedio de las tres notas opcionales de un local (#336), y cuántas reseñas las
 * pusieron.
 *
 * Son opcionales, así que cada una tiene su propia cuenta: un promedio de Atención sobre
 * dos reseñas no se lee igual que uno sobre doscientas. Sin ninguna, el promedio es nulo
 * y la cuenta cero; quien lo muestra tiene que estar listo para no mostrar nada.
 */
public record DesgloseDeNotasDto(
    Double local,
    Long cuantasLocal,
    Double atencion,
    Long cuantasAtencion,
    Double precio,
    Long cuantasPrecio
) {}
