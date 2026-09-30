package com.hamburguesas.places;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;

/**
 * En qué zona cae un local: el barrio si está en la Ciudad, la localidad si está afuera.
 *
 * La app empezó siendo de la Ciudad y el barrio salía de los límites oficiales, que son
 * exactos y no cuestan una llamada. Al abrir la búsqueda al conurbano eso ya no alcanza:
 * no hay un archivo de límites de los partidos cargado, y bajar uno sería arrastrar un
 * dataset grande para resolver algo que Google ya contesta.
 *
 * Así que afuera de la Ciudad la zona sale de la dirección, que para la Provincia viene
 * siempre con la misma forma:
 *
 *     Brandsen 1204, B1814 Cañuelas, Provincia de Buenos Aires, Argentina
 *
 * El anteúltimo tramo antes de la provincia es la localidad, con el código postal
 * pegado adelante.
 *
 * El radio es lo que decide hasta dónde se busca. Con 75 km desde el Obelisco entran
 * La Plata, San Vicente, Cañuelas, Luján y Belén de Escobar, que son los bordes que
 * queríamos cubrir.
 */
@Component
@RequiredArgsConstructor
public class Zonas {

    /** El Obelisco, que es desde donde se mide. */
    private static final double LATITUD_DEL_CENTRO = -34.6037;
    private static final double LONGITUD_DEL_CENTRO = -58.3816;

    private static final double RADIO_DE_LA_TIERRA_KM = 6371.0;

    private final Barrios barrios;
    private final PlacesProperties properties;

    /**
     * @return el barrio o la localidad, o vacío si el local queda fuera del radio o no
     *         se le puede sacar una zona a la dirección
     */
    public Optional<String> zonaDe(Double latitud, Double longitud, String direccion) {
        Optional<String> barrio = barrios.barrioDe(latitud, longitud);
        if (barrio.isPresent()) {
            return barrio;
        }

        if (latitud == null || longitud == null) {
            return Optional.empty();
        }

        if (kilometrosDesdeElCentro(latitud, longitud) > properties.getSync().getRadioEnKm()) {
            return Optional.empty();
        }

        return localidadDe(direccion);
    }

    /**
     * La localidad que declara la dirección.
     *
     * Se toma de la dirección y no de las coordenadas porque es lo que Google ya nos
     * mandó: deducirla del punto pediría los límites de cada partido, que es un archivo
     * que no tenemos y una precisión que esta pantalla no necesita.
     */
    static Optional<String> localidadDe(String direccion) {
        if (direccion == null || direccion.isBlank()) {
            return Optional.empty();
        }

        String[] tramos = direccion.split(",");
        // Los dos últimos son la provincia y el país; la localidad es el de antes.
        if (tramos.length < 3) {
            return Optional.empty();
        }

        String tramo = tramos[tramos.length - 3].trim();
        String localidad = sinCodigoPostal(tramo);
        return localidad.isBlank() ? Optional.empty() : Optional.of(localidad);
    }

    /**
     * Saca el código postal del principio: "B1814 Cañuelas" queda en "Cañuelas".
     *
     * El formato argentino es una letra, cuatro números y a veces tres letras más. Se
     * pide que empiece con letra y traiga números para no comerse el nombre de una
     * localidad que arranque con una palabra corta.
     */
    private static String sinCodigoPostal(String tramo) {
        String[] palabras = tramo.split(" ", 2);
        if (palabras.length == 2 && pareceCodigoPostal(palabras[0])) {
            return palabras[1].trim();
        }
        return tramo;
    }

    private static boolean pareceCodigoPostal(String palabra) {
        return palabra.matches("[A-Za-z][0-9]{4}[A-Za-z]{0,3}")
            || palabra.matches("[0-9]{4}");
    }

    /**
     * Distancia en línea recta, por la fórmula del semiverseno.
     *
     * Alcanza de sobra para un radio de decenas de kilómetros, y no cuesta ninguna
     * llamada: las coordenadas ya vienen con el local.
     */
    static double kilometrosDesdeElCentro(double latitud, double longitud) {
        double dLat = Math.toRadians(latitud - LATITUD_DEL_CENTRO);
        double dLon = Math.toRadians(longitud - LONGITUD_DEL_CENTRO);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(LATITUD_DEL_CENTRO)) * Math.cos(Math.toRadians(latitud))
            * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return RADIO_DE_LA_TIERRA_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /** Para los mensajes, que se leen en español. */
    static String enMinusculas(String valor) {
        return valor == null ? "" : valor.toLowerCase(Locale.ROOT);
    }
}
