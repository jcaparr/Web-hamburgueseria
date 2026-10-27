package com.hamburguesas.service;

/**
 * Los premios de la hamburguesa que se desbloquean con el nivel (#313).
 *
 * El código de la hamburguesa tiene cinco cifras (fondo, pan, queso, verdura, carnes) y,
 * si se usa algún premio, tres más al final: huevo, bacon y fondo de rayos, cada una 0
 * o 1. La cifra de las carnes llega hasta 4: la cuarta es el premio del nivel 10.
 *
 * Cuenta pura, sin Spring: lo que hay que poder probar es qué se puede usar a cada nivel.
 * La tabla de niveles es la de {@code frontend/src/utils/niveles.ts} y el desbloqueo de
 * {@code frontend/src/utils/recetas.ts}.
 */
public final class Premios {

    public static final int NIVEL_DEL_HUEVO = 3;
    public static final int NIVEL_DEL_BACON = 5;
    public static final int NIVEL_DE_LA_CUARTA_CARNE = 10;
    public static final int NIVEL_DE_LOS_RAYOS = 15;

    private static final int LARGO_BASE = 5;
    private static final int LARGO_CON_PREMIOS = 8;

    private Premios() {
    }

    /**
     * Qué de lo pedido todavía no está desbloqueado, dicho para mostrarlo; null si todo
     * está al alcance. Se mira lo primero que falla, en el orden en que se ganan.
     */
    public static String bloqueado(String receta, int nivel) {
        if (receta == null) {
            return null;
        }
        if (usa(receta, 5) && nivel < NIVEL_DEL_HUEVO) {
            return "Todavía no desbloqueaste el huevo";
        }
        if (usa(receta, 6) && nivel < NIVEL_DEL_BACON) {
            return "Todavía no desbloqueaste el bacon";
        }
        if (receta.charAt(4) == '4' && nivel < NIVEL_DE_LA_CUARTA_CARNE) {
            return "Todavía no desbloqueaste la cuarta carne";
        }
        if (usa(receta, 7) && nivel < NIVEL_DE_LOS_RAYOS) {
            return "Todavía no desbloqueaste el fondo de rayos";
        }
        return null;
    }

    /**
     * La misma hamburguesa sin lo que ya no alcanza: huevo 0, bacon 0, cuatro carnes pasa
     * a tres y sin rayos. Si no queda ningún premio vuelve a las cinco cifras.
     */
    public static String recortar(String receta, int nivel) {
        if (receta == null) {
            return null;
        }
        char[] cifras = receta.toCharArray();
        if (nivel < NIVEL_DEL_HUEVO && cifras.length == LARGO_CON_PREMIOS) {
            cifras[5] = '0';
        }
        if (nivel < NIVEL_DEL_BACON && cifras.length == LARGO_CON_PREMIOS) {
            cifras[6] = '0';
        }
        if (nivel < NIVEL_DE_LA_CUARTA_CARNE && cifras[4] == '4') {
            cifras[4] = '3';
        }
        if (nivel < NIVEL_DE_LOS_RAYOS && cifras.length == LARGO_CON_PREMIOS) {
            cifras[7] = '0';
        }
        String resultado = new String(cifras);
        if (resultado.length() == LARGO_CON_PREMIOS && resultado.endsWith("000")) {
            return resultado.substring(0, LARGO_BASE);
        }
        return resultado;
    }

    private static boolean usa(String receta, int indice) {
        return receta.length() == LARGO_CON_PREMIOS && receta.charAt(indice) == '1';
    }
}
