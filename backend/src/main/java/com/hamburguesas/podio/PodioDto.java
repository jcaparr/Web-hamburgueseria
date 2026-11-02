package com.hamburguesas.podio;

import java.time.Instant;
import java.util.List;

/**
 * El podio de personas de un período (#318): el top y el puesto de quien mira.
 *
 * @param cierraEl     cuándo cierra el período (el mismo límite del cierre real); null en General
 * @param xpDelPeriodo la XP ganada en el período (en General, la XP total)
 * @param xp           la XP total de la persona, para dibujar su nivel
 */
public record PodioDto(String periodo, Instant cierraEl, List<Puesto> top, Puesto yo) {

    /**
     * Un período ya cerrado (#382): de {@code desde} a {@code hasta}, ambos inclusive, con quienes
     * salieron en los primeros puestos. Es el resultado guardado al cerrar; no se recalcula.
     */
    public record Anterior(java.time.LocalDate desde, java.time.LocalDate hasta, List<Puesto> top) {
    }

    public record Puesto(int puesto, Long userId, String username, String hamburguesa, int xp, int xpDelPeriodo) {
    }
}
