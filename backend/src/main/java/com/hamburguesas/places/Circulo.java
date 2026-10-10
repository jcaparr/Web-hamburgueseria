package com.hamburguesas.places;

/**
 * Un radio alrededor de un punto, para barrer una zona que no tiene un nombre que Google
 * entienda: "10 km alrededor de Pilar" (#222).
 *
 * A Google se le pide que priorice el círculo, pero es una preferencia y no un límite:
 * igual contesta con lo que le parece cerca. Por eso además se descarta lo que cae afuera.
 */
public record Circulo(double latitud, double longitud, double radioKm) {

    /**
     * El radio más grande que acepta Google para priorizar un círculo: 50 km. Uno más
     * grande se le pide recortado, y lo que entra lo sigue decidiendo el radio pedido.
     */
    static final double RADIO_MAXIMO_PARA_GOOGLE_KM = 50;

    boolean contiene(Double lat, Double lng) {
        return lat != null && lng != null
            && Zonas.kilometrosEntre(latitud, longitud, lat, lng) <= radioKm;
    }

    double radioParaGoogleEnMetros() {
        return Math.min(radioKm, RADIO_MAXIMO_PARA_GOOGLE_KM) * 1000;
    }
}
