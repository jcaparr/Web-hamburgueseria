package com.hamburguesas.repository;

import java.util.Locale;

/**
 * Cómo se ordena Explorar: las tres opciones que se pueden elegir (#207).
 *
 * Se elige por nombre y no con un "sort" libre: el servidor ignora cualquier orden que
 * mande el cliente (#201), y estas tres son las únicas que existen.
 *
 * En las tres, los locales sin reseñas van al final y por nombre. Si no, "peores
 * valoradas" arrancaría con los mil que nadie probó.
 */
public enum OrdenDeLocales {

    /**
     * Más reseñas primero; a igual cantidad, mejor puntaje. Es el de por omisión: con
     * "mejores valoradas", uno con una sola reseña de 5 queda arriba de uno con veinte y
     * 4,8 de promedio, y la cantidad es lo que más dice de un lugar.
     */
    RELEVANTES,

    /** Mejor puntaje primero; a igual puntaje, más reseñas. */
    MEJORES,

    /** Peor puntaje primero; a igual puntaje, más reseñas. */
    PEORES;

    /**
     * La opción que dice la dirección, sin importar mayúsculas: "mejores", "MEJORES".
     *
     * Una que no existe es la de por omisión y no un error. Es lo que pasa con un enlace
     * guardado de una versión con otras opciones, y quien lo abre quiere ver la lista,
     * no un mensaje.
     */
    public static OrdenDeLocales de(String valor) {
        if (valor == null || valor.isBlank()) {
            return RELEVANTES;
        }
        try {
            return valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return RELEVANTES;
        }
    }
}
