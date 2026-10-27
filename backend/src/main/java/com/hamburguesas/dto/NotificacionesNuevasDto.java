package com.hamburguesas.dto;

/**
 * Cuántos avisos hay sin ver, para el número de la campana (#210).
 *
 * @param logrosSinVer los logros ganados cuyo aviso falta mostrar (#317): prende el punto de
 *                    Logros y le dice a la pantalla que pida las novedades. Los de la carga
 *                    inicial cuentan como uno solo.
 */
public record NotificacionesNuevasDto(long nuevas, long logrosSinVer) {}
