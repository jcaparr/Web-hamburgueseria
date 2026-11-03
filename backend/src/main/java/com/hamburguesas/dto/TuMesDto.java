package com.hamburguesas.dto;

import java.util.List;

/**
 * El resumen de un mes de una persona, "Tu mes en Burgómetro" (#308).
 *
 * Con cero reseñas no hay resumen: las tarjetas no se arman y la pantalla no ofrece nada.
 *
 * @param mes          el mes que resume, "2026-11"
 * @param barrios      los barrios con más reseñas del mes, hasta tres
 * @param barriosEnTotal cuántos barrios distintos visitó, para el "+N"
 * @param mejor        la reseña con la nota más alta del mes
 * @param nivel        el nivel de hoy, no el del fin del mes: la XP no guarda historia
 * @param logros       cuántos logros ganó durante el mes
 * @param anio         solo en el resumen de diciembre: lo que hizo en todo el año
 */
public record TuMesDto(
    String mes,
    int resenias,
    List<Barrio> barrios,
    int barriosEnTotal,
    Mejor mejor,
    int nivel,
    int xp,
    int logros,
    Anio anio
) {

    public record Barrio(String nombre, int resenias) {}

    public record Mejor(Long localId, String slug, String nombre, String barrio, String foto, int puntaje) {}

    public record Anio(int resenias, int locales) {}
}
