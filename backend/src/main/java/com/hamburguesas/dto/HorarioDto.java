package com.hamburguesas.dto;

import java.util.List;

/**
 * El horario de un local, para que el navegador diga si está abierto ahora.
 *
 * Abierto o cerrado se calcula allá y no acá: depende de la hora en que se mira, y
 * así la respuesta es la misma todo el día.
 *
 * @param franjas    los tramos abiertos de la semana, ordenados por día y apertura.
 *                   Vacía si Google no tiene el horario o si todavía no se preguntó.
 * @param consultado si ya se le preguntó a Google. Con franjas vacías distingue "Google
 *                   no lo tiene" de "todavía no lo pedimos".
 */
public record HorarioDto(List<Franja> franjas, boolean consultado) {

    /**
     * Un tramo abierto.
     *
     * @param dia    0 es domingo y 6 es sábado
     * @param abre   minutos desde la medianoche de ese día
     * @param cierra minutos desde la misma medianoche: pasa de 1440 si cierra al día siguiente
     */
    public record Franja(int dia, int abre, int cierra) {}
}
