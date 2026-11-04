package com.hamburguesas.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * El resumen de un mes de una persona, "Tu mes en Burgómetro" (#308). El de diciembre es el
 * recuento de todo el año, "Tu 2026 en Burgómetro": mismas tarjetas, calculadas sobre el año.
 *
 * Con cero reseñas no hay resumen: las tarjetas no se arman y la pantalla no ofrece nada.
 *
 * @param mes          el mes que resume, "2026-11"; en el anual, "2026-12"
 * @param anual        si es el recuento del año entero (el resumen de diciembre)
 * @param reseniasAnteriores las reseñas del período anterior (el mes anterior, o el año anterior en el anual)
 * @param barrios      los barrios con más reseñas del período, hasta tres
 * @param barriosEnTotal cuántos barrios distintos visitó, para el "+N"
 * @param mejor        la reseña con la nota más alta del período
 * @param nivel        el nivel de hoy, no el del fin del período: para eso está {@code recuento}
 * @param logros       cuántos logros ganó durante el período
 * @param logrosGanados cuáles, del primero al último; los secretos van con su código y se muestran
 * @param podios       los puestos que sacó en los podios que empezaron en el período, solo los de esta persona
 * @param dia          el día de la semana (y la franja) en que más reseñó; nulo con menos de tres reseñas
 * @param reacciones   las que recibieron sus reseñas, sin las de personas bloqueadas; nulo si no hubo
 * @param recuento     solo en el anual: el mes más activo y el nivel con el que empezó y terminó el año
 */
public record TuMesDto(
    String mes,
    boolean anual,
    int resenias,
    int reseniasAnteriores,
    List<Barrio> barrios,
    int barriosEnTotal,
    Mejor mejor,
    int nivel,
    int xp,
    int logros,
    List<LogroGanado> logrosGanados,
    List<Puesto> podios,
    Dia dia,
    Reacciones reacciones,
    Recuento recuento
) {

    public record Barrio(String nombre, int resenias) {}

    public record Mejor(Long localId, String slug, String nombre, String barrio, String foto, int puntaje) {}

    /** Un logro ganado: su código y, si lo lleva, el detalle (el barrio del Intendente). */
    public record LogroGanado(String codigo, String detalle) {}

    /** Un puesto en un podio: "semana" o "mes", desde qué día y en qué puesto salió. */
    public record Puesto(String periodo, LocalDate desde, int puesto) {}

    /** El día favorito: de 1 (lunes) a 7 (domingo), y la franja: madrugada, mañana, tarde o noche. */
    public record Dia(int diaDeLaSemana, String franja) {}

    /** Las reacciones recibidas: cuántas y cuál fue la más usada (el nombre del tipo). */
    public record Reacciones(int total, String masUsada) {}

    /** El mes más activo (1 a 12, con sus reseñas) y los niveles de principio y fin de año. */
    public record Recuento(int mesMasActivo, int reseniasDelMes, int nivelAlEmpezar, int nivelAlTerminar) {}
}
