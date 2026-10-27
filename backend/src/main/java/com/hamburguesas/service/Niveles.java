package com.hamburguesas.service;

/**
 * La XP y los niveles (#310). Solo las reseñas dan XP.
 *
 * Es una cuenta pura, sin base ni Spring, porque lo que hay que poder probar es la
 * tabla y las reglas. La tabla de umbrales también vive en el frontend
 * ({@code utils/niveles.ts}): si cambia acá, cambia allá, y un test fija esta.
 */
public final class Niveles {

    /** XP mínima de cada nivel: el nivel 1 arranca en 0 y el 20 en 6.500. */
    public static final int[] UMBRALES = {
        0, 100, 200, 400, 600, 800, 1000, 1100, 1300, 1500,
        2000, 2500, 3000, 3500, 4000, 4500, 5000, 5500, 6000, 6500
    };

    public static final int NIVEL_MAXIMO = UMBRALES.length;

    /** Lo que da una reseña con solo la foto. */
    public static final int XP_CON_FOTO = 70;
    /** Lo que da una reseña con un comentario de verdad. */
    public static final int XP_CON_COMENTARIO = 100;
    /** Se suma una vez a quien tiene al menos una reseña. */
    public static final int XP_DE_BIENVENIDA = 30;

    /** Un comentario cuenta desde estas palabras (#344); no hay mínimo de letras. */
    public static final int PALABRAS_MINIMAS = 5;

    private Niveles() {
    }

    /** El nivel de 1 a 20 que le toca a esta XP. La XP sigue sumando después del 20. */
    public static int nivelDe(int xp) {
        int nivel = 1;
        for (int i = 1; i < UMBRALES.length; i++) {
            if (xp >= UMBRALES[i]) {
                nivel = i + 1;
            }
        }
        return nivel;
    }

    /** Cuánta XP da una reseña con este comentario (puede ser nulo o vacío). */
    public static int xpDe(String comentario) {
        return tieneComentario(comentario) ? XP_CON_COMENTARIO : XP_CON_FOTO;
    }

    /**
     * XP total de quien tiene estas reseñas: lo de cada una más el bono de bienvenida si
     * tiene alguna. Es la única fuente de la cuenta; la migración V37 hace lo mismo en SQL.
     */
    public static int xpTotal(Iterable<String> comentarios) {
        int total = 0;
        boolean alguna = false;
        for (String comentario : comentarios) {
            total += xpDe(comentario);
            alguna = true;
        }
        return alguna ? total + XP_DE_BIENVENIDA : 0;
    }

    /**
     * Un comentario cuenta si tiene 5 palabras. Una palabra es un tramo entre espacios que
     * tenga al menos una letra: los números y los signos no lo son, así que "! ! ! ! !" no
     * puede comprar los 30 puntos de diferencia.
     */
    static boolean tieneComentario(String comentario) {
        if (comentario == null) {
            return false;
        }
        String limpio = comentario.strip();
        if (limpio.isEmpty()) {
            return false;
        }
        long palabras = java.util.Arrays.stream(limpio.split("\\s+"))
            .filter(palabra -> palabra.chars().anyMatch(Character::isLetter))
            .count();
        return palabras >= PALABRAS_MINIMAS;
    }
}
