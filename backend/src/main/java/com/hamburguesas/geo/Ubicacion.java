package com.hamburguesas.geo;

/**
 * Dónde está quien mira la lista, tal como la manda su navegador.
 *
 * Nunca se guarda: llega con el pedido, ordena la respuesta y se descarta.
 */
public record Ubicacion(double latitud, double longitud) {

    /**
     * Una ubicación solo si vinieron las dos coordenadas y son de este planeta.
     *
     * Media ubicación o una fuera de rango no es un error: quien abre la dirección
     * quiere ver la lista, y sin ubicación se ordena como siempre.
     */
    public static Ubicacion de(Double latitud, Double longitud) {
        if (latitud == null || longitud == null
            || !Double.isFinite(latitud) || !Double.isFinite(longitud)
            || Math.abs(latitud) > 90 || Math.abs(longitud) > 180) {
            return null;
        }
        return new Ubicacion(latitud, longitud);
    }
}
