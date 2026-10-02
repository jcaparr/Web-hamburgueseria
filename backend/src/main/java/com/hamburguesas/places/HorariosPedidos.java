package com.hamburguesas.places;

/**
 * Qué hizo una pasada de horarios.
 *
 * @param conHorario los que quedaron con su horario guardado
 * @param sinHorario los que Google contestó que no tiene horario
 * @param fallidos   los que Google no contestó: quedan para la próxima pasada
 * @param faltan     los que no se llegaron a preguntar porque se terminó la cuota del mes
 * @param aviso      por qué frenó antes de terminar, o null si terminó
 */
public record HorariosPedidos(int conHorario, int sinHorario, int fallidos, int faltan,
                              String aviso) {

    static HorariosPedidos skipped(String motivo) {
        return new HorariosPedidos(0, 0, 0, 0, motivo);
    }
}
