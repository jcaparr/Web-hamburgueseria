package com.hamburguesas.texto;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Las formas de normalizar un texto antes de compararlo.
 *
 * Estaban copiadas en nueve lugares, en tres variantes que se parecían lo suficiente como
 * para confundirlas y no lo suficiente como para intercambiarlas. Juntas acá, cada una con
 * el nombre de lo que hace, elegir la equivocada se ve.
 *
 * Las tres sacan los acentos y pasan a minúscula, porque Google escribe el mismo nombre de
 * varias maneras —"Wendy's" y "Wendys", "Núñez" y "Nunez"— y nadie los escribe en un
 * buscador. Lo que cambia es qué hacen con el resto.
 */
public final class Texto {

    private Texto() {
    }

    /**
     * Sin acentos, en minúscula y con los espacios de más colapsados.
     *
     * Para comparar nombres conservando las palabras: "Dean & Dennys  - Palermo" y
     * "dean & dennys - palermo" quedan iguales, y los espacios siguen marcando dónde
     * termina una palabra, que es lo que hace falta para reconocer el principio de un
     * nombre.
     *
     * @return el texto normalizado, o vacío si era nulo
     */
    public static String paraComparar(String valor) {
        return sinAcentosEnMinuscula(valor)
            .replaceAll("\\s+", " ")
            .trim();
    }

    /**
     * Solo letras y números, sin acentos y en minúscula.
     *
     * Para reconocer una marca escrita de cualquier forma: "Wendy's", "Wendys" y
     * "WENDY S" dan todos "wendys". Se pierden los espacios, así que sirve para ver si un
     * nombre empieza con otro, no para separarlo en palabras.
     *
     * @return el texto compactado, o vacío si era nulo
     */
    public static String soloLetrasYNumeros(String valor) {
        return sinAcentosEnMinuscula(valor).replaceAll("[^a-z0-9]", "");
    }

    /**
     * Sin acentos y en minúscula, y nada más: los espacios y los signos quedan.
     *
     * Para buscar palabras dentro de un texto más largo, donde los signos le dicen a la
     * expresión regular dónde termina cada una.
     *
     * @return el texto sin acentos, o vacío si era nulo
     */
    public static String sinAcentosEnMinuscula(String valor) {
        return Normalizer.normalize(valor == null ? "" : valor, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT);
    }
}
