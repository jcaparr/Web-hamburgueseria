package com.hamburguesas.places;

import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Lee el horario que devuelve Google y lo deja en franjas de un día cada una.
 *
 * Vive aparte del cliente, sin Spring ni red, porque los casos raros son justamente los
 * que hay que poder probar: el local que cierra pasada la medianoche, el que abre las
 * veinticuatro horas y el que Google no sabe cuándo abre.
 */
final class HorarioDeGoogle {

    static final int MINUTOS_POR_DIA = 24 * 60;
    private static final int MINUTOS_POR_SEMANA = 7 * MINUTOS_POR_DIA;

    /**
     * Un tramo abierto que empieza un día de la semana.
     *
     * @param dia    0 es domingo y 6 es sábado, como los numera Google
     * @param abre   minutos desde la medianoche de ese día
     * @param cierra minutos desde la misma medianoche: pasa de 1440 si cierra al día siguiente
     */
    record Franja(int dia, int abre, int cierra) {}

    private HorarioDeGoogle() {
    }

    /**
     * Las franjas de la semana, ordenadas por día y hora de apertura.
     *
     * @param place la ficha que devolvió Google, con el campo regularOpeningHours
     * @return vacía si Google no tiene el horario del local
     */
    static List<Franja> franjas(JsonNode place) {
        JsonNode periodos = place == null ? null : place.path("regularOpeningHours").path("periods");
        if (periodos == null || !periodos.isArray() || periodos.isEmpty()) {
            return List.of();
        }

        // Así dice Google "abierto siempre": una sola apertura el domingo a medianoche y
        // ningún cierre. Se guarda como los siete días enteros, que es lo que se lee.
        if (periodos.size() == 1 && abreSiempre(periodos.get(0))) {
            List<Franja> todaLaSemana = new ArrayList<>();
            for (int dia = 0; dia < 7; dia++) {
                todaLaSemana.add(new Franja(dia, 0, MINUTOS_POR_DIA));
            }
            return todaLaSemana;
        }

        List<Franja> franjas = new ArrayList<>();
        for (JsonNode periodo : periodos) {
            JsonNode apertura = periodo.path("open");
            JsonNode cierre = periodo.path("close");
            // Una apertura sin cierre que no es el "abierto siempre" no dice hasta cuándo:
            // inventarle un cierre sería mostrar un horario que el local no tiene.
            if (apertura.isMissingNode() || cierre.isMissingNode()) {
                continue;
            }

            int dia = apertura.path("day").asInt();
            int abre = minutos(apertura);
            int diasHastaQueCierra = Math.floorMod(cierre.path("day").asInt() - dia, 7);
            int cierra = diasHastaQueCierra * MINUTOS_POR_DIA + minutos(cierre);

            // Cierra a la misma hora que abre, el mismo día: es una semana entera abierto.
            if (cierra <= abre) {
                cierra += MINUTOS_POR_SEMANA;
            }
            franjas.add(new Franja(dia, abre, cierra));
        }

        franjas.sort(Comparator.comparingInt(Franja::dia).thenComparingInt(Franja::abre));
        return franjas;
    }

    private static boolean abreSiempre(JsonNode periodo) {
        JsonNode apertura = periodo.path("open");
        return periodo.path("close").isMissingNode()
            && apertura.path("day").asInt(-1) == 0
            && minutos(apertura) == 0;
    }

    private static int minutos(JsonNode momento) {
        return momento.path("hour").asInt() * 60 + momento.path("minute").asInt();
    }
}
