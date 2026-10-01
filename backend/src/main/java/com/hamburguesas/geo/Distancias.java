package com.hamburguesas.geo;

/**
 * Distancias entre dos puntos de la Ciudad.
 *
 * La Tierra es redonda y acá se la trata como un plano. A las distancias que maneja la
 * app —de una cuadra a los veinte kilómetros que mide la Ciudad de punta a punta— el
 * error de esa simplificación es de centímetros, y ahorra la trigonometría de la
 * fórmula del semiverseno.
 *
 * Los dos números de abajo son cuánto mide un grado en metros a la latitud de Buenos
 * Aires. El de longitud se achica a medida que uno se aleja del ecuador; el de latitud
 * es casi constante en todo el planeta.
 */
public final class Distancias {

    private static final double METROS_POR_GRADO_DE_LONGITUD = 91_700;
    private static final double METROS_POR_GRADO_DE_LATITUD = 111_000;

    private Distancias() {}

    /** En línea recta, sin mirar las calles. */
    public static double metrosEntre(double latA, double lonA, double latB, double lonB) {
        double x = (lonA - lonB) * METROS_POR_GRADO_DE_LONGITUD;
        double y = (latA - latB) * METROS_POR_GRADO_DE_LATITUD;
        return Math.hypot(x, y);
    }

    /**
     * Con un decimal, que es como se muestran: "1,3 km" se lee, "1,2847 km" no, y los
     * metros de diferencia no cambian ninguna decisión de quien camina.
     */
    public static double aUnDecimal(double kilometros) {
        return Math.round(kilometros * 10) / 10.0;
    }
}
