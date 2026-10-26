package com.hamburguesas.logros;

import java.util.List;

/**
 * Lo que hay para avisar de los logros (#317).
 *
 * @param xp          la XP de ahora, para que el aviso sepa en qué nivel está
 * @param sinVer      los códigos ganados cuyo aviso todavía no se mostró
 * @param retroactivos cuántos de la carga inicial hay sin ver: se avisan juntos, con un solo
 *                    mensaje ("Ya tenías N logros"), y no figuran en {@code sinVer}
 */
public record NovedadesDeLogros(int xp, List<String> sinVer, long retroactivos) {
}
