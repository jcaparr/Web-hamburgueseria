package com.hamburguesas.service;

import com.hamburguesas.service.TemasDeLasResenias.Tema;
import com.hamburguesas.service.TemasDeLasResenias.Tono;
import java.util.List;
import java.util.Locale;

/**
 * Una oración que resume de qué hablan las reseñas, como la que muestra Google Maps (#392).
 *
 * No la escribe una IA: se arma con los mismos temas que se muestran abajo, ya contados,
 * y unas pocas plantillas. Así no cuesta nada, no llama a nadie y no puede decir algo que
 * los números de abajo no digan. Lo que se pierde es la soltura de un texto escrito; lo
 * que se gana es que cada palabra se puede auditar contra los chips.
 */
public final class FraseDeLasResenias {

    private FraseDeLasResenias() {}

    /** Más de tres en una enumeración ya es una lista, y la lista está abajo. */
    private static final int A_FAVOR_EN_LA_FRASE = 3;
    private static final int EN_CONTRA_EN_LA_FRASE = 2;

    /**
     * La oración, o null si no hay temas a favor ni en contra: sin eso no hay nada que
     * resumir, y una frase genérica ("la gente opina cosas") es peor que ninguna.
     *
     * Lo dividido no se nombra, igual que en la pantalla (#392): Juan lo sacó porque no
     * ayuda a decidir.
     */
    public static String de(List<Tema> temas) {
        List<String> aFavor = nombres(temas, Tono.A_FAVOR, A_FAVOR_EN_LA_FRASE);
        List<String> enContra = nombres(temas, Tono.EN_CONTRA, EN_CONTRA_EN_LA_FRASE);

        StringBuilder frase = new StringBuilder();
        if (!aFavor.isEmpty() && !enContra.isEmpty()) {
            frase.append("Quienes escribieron destacan ").append(enumerar(aFavor))
                .append(", pero se quejan ").append(conDe(enContra)).append('.');
        } else if (!aFavor.isEmpty()) {
            frase.append("Quienes escribieron destacan ").append(enumerar(aFavor)).append('.');
        } else if (!enContra.isEmpty()) {
            frase.append("Quienes escribieron se quejan ").append(conDe(enContra)).append('.');
        }

        return frase.isEmpty() ? null : frase.toString();
    }

    /** En minúscula porque van en medio de la oración: "La carne" pasa a "la carne". */
    private static List<String> nombres(List<Tema> temas, Tono tono, int cuantos) {
        return temas.stream()
            .filter(tema -> tema.tono() == tono)
            .limit(cuantos)
            .map(tema -> tema.nombre().toLowerCase(Locale.ROOT))
            .toList();
    }

    /** "a", "a y b", "a, b y c". Todos empiezan con artículo, así que nunca hace falta "e". */
    private static String enumerar(List<String> nombres) {
        if (nombres.size() == 1) {
            return nombres.get(0);
        }
        return String.join(", ", nombres.subList(0, nombres.size() - 1))
            + " y " + nombres.get(nombres.size() - 1);
    }

    /** "de la espera", y "del precio" y no "de el precio". */
    private static String conDe(List<String> nombres) {
        String lista = enumerar(nombres);
        return lista.startsWith("el ") ? "del " + lista.substring(3) : "de " + lista;
    }
}
