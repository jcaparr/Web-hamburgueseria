package com.hamburguesas.podio;

import java.util.List;

/**
 * El podio de personas de un período (#318): el top y el puesto de quien mira.
 *
 * @param xpDelPeriodo la XP ganada en el período (en General, la XP total)
 * @param xp           la XP total de la persona, para dibujar su nivel
 */
public record PodioDto(String periodo, List<Puesto> top, Puesto yo) {

    public record Puesto(int puesto, Long userId, String username, String hamburguesa, int xp, int xpDelPeriodo) {
    }
}
