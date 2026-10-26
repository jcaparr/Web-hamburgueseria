package com.hamburguesas.logros;

import java.time.Instant;
import java.util.List;

/**
 * Lo que necesita la pantalla de Logros (#316): dónde está la cuenta en los niveles y cada
 * logro del catálogo con si lo ganó, cuándo y cuánto le falta.
 *
 * @param xpDelNivel     la XP en que empezó el nivel actual
 * @param xpDelSiguiente la XP en que empieza el siguiente, o null en el último nivel
 */
public record PantallaDeLogros(int nivel, int xp, int xpDelNivel, Integer xpDelSiguiente, List<Item> logros) {

    /**
     * @param codigo  el código estable; los nombres y los textos viven en el frontend
     * @param ganado  si lo tiene activo hoy
     * @param fecha   cuándo lo ganó (solo si lo tiene)
     * @param actual  el avance en los que se miden con un número; con {@code meta}, nulo si no se mide
     * @param detalle un dato del logro, como el barrio del Intendente
     */
    public record Item(String codigo, boolean ganado, Instant fecha, Integer actual, Integer meta, String detalle) {
    }
}
