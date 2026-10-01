package com.hamburguesas.places;

import com.hamburguesas.texto.Texto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

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

        // A veces Google no manda localidad y ese tramo es el código postal solo:
        // "Av. Bartolomé Mitre 666, B1870 AAT, Cdad. Autónoma de Buenos Aires, Argentina".
        // Sacarle el código postal dejaba "AAT" de nombre de zona, y eso terminaba en el
        // selector de Explorar como si fuera un lugar.
        //
        // No se devuelve vacío: el local existe y está dentro del radio, y vacío lo
        // borraría. Va a una zona genérica, que es lo poco cierto que se puede decir.
        if (localidad.isBlank() || esLoQueQuedaDeUnCodigoPostal(localidad)) {
            return Optional.of(SIN_LOCALIDAD);
        }

        return Optional.of(comoSeLlamaDeVerdad(localidad));
    }

    /**
     * Para los locales cuya dirección no dice en qué localidad están.
     *
     * Son pocos y no se pueden ubicar mejor sin pedirle otra cosa a Google. Juntarlos acá
     * es más honesto que inventarles una localidad, y los deja encontrables.
     */
    static final String SIN_LOCALIDAD = "Gran Buenos Aires";

    /**
     * Si lo que quedó después de sacar el código postal es el resto del mismo código.
     *
     * El código argentino largo es "B1870AAT": una letra, cuatro números y tres letras.
     * Google a veces lo escribe partido —"B1870 AAT"— y entonces sacarle la primera mitad
     * deja la segunda, que no es el nombre de ningún lugar.
     *
     * Se pide que sean todas mayúsculas para no confundirlo con una localidad corta de
     * verdad: ninguna se escribe gritando.
     */
    private static boolean esLoQueQuedaDeUnCodigoPostal(String localidad) {
        return localidad.matches("[A-Z]{1,3}") || localidad.matches("[0-9]{4}");
    }

    /**
     * El nombre con el que la localidad va a aparecer en el selector.
     *
     * Google escribe la misma localidad de varias maneras y cada variante se convertía en
     * una opción distinta: "ezeiza" y "Ezeiza", "Almte. Brown" y "Almirante Brown",
     * "3 de Febrero" y "Tres de Febrero". Con doscientas zonas en la lista, los
     * duplicados son lo que la vuelve inservible.
     */
    private static String comoSeLlamaDeVerdad(String localidad) {
        String limpia = sinSufijoDeRegion(localidad);
        String conocida = COMO_SE_ESCRIBE.get(Texto.sinAcentosEnMinuscula(limpia));
        return conocida != null ? conocida : conMayusculaInicial(limpia);
    }

    /**
     * Las formas abreviadas que Google mezcla con las completas.
     *
     * Se buscan sin acentos ni mayúsculas, así que una sola entrada cubre todas las
     * maneras de escribirla. Son las que aparecieron de verdad en la base; la lista crece
     * cuando aparece otra.
     */
    private static final Map<String, String> COMO_SE_ESCRIBE = Map.ofEntries(
        Map.entry("almte. brown", "Almirante Brown"),
        Map.entry("gral. rodriguez", "General Rodríguez"),
        Map.entry("gral. pacheco", "General Pacheco"),
        Map.entry("gral. las heras", "General Las Heras"),
        Map.entry("3 de febrero", "Tres de Febrero"),
        Map.entry("gdor. costa", "Gobernador Costa"),
        Map.entry("cdad. evita", "Ciudad Evita"),
        Map.entry("i.casanova", "Isidro Casanova"),
        Map.entry("pres. derqui", "Presidente Derqui"),
        Map.entry("san jose", "San José"),
        Map.entry("gran buenos aires", SIN_LOCALIDAD),
        Map.entry("buenos aires", SIN_LOCALIDAD)
    );

    /** "Lomas de Zamora - GBA Sur" y "Lomas de Zamora" son el mismo lugar. */
    private static String sinSufijoDeRegion(String localidad) {
        int guion = localidad.indexOf(" - ");
        return guion > 0 ? localidad.substring(0, guion).trim() : localidad;
    }

    /**
     * Mayúscula al principio de cada palabra, salvo las que nunca la llevan.
     *
     * Es lo que junta "ezeiza" con "Ezeiza" sin tener que anotar cada localidad a mano:
     * las dos terminan escritas igual. Las partículas quedan en minúscula para no
     * escribir "Lomas De Zamora".
     */
    private static String conMayusculaInicial(String localidad) {
        String[] palabras = localidad.trim().split("\\s+");
        StringBuilder armada = new StringBuilder();
        for (int i = 0; i < palabras.length; i++) {
            String palabra = palabras[i];
            if (palabra.isEmpty()) {
                continue;
            }
            if (!armada.isEmpty()) {
                armada.append(' ');
            }
            if (i > 0 && PARTICULAS.contains(Texto.sinAcentosEnMinuscula(palabra))) {
                armada.append(Texto.sinAcentosEnMinuscula(palabra).equals(palabra)
                    ? palabra : palabra.toLowerCase(Locale.ROOT));
                continue;
            }
            armada.append(Character.toUpperCase(palabra.charAt(0)))
                .append(palabra.substring(1));
        }
        return armada.toString();
    }

    private static final Set<String> PARTICULAS = Set.of("de", "del", "la", "las", "los", "y");

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
            return sinLoQueQuedoPegado(palabras[1].trim());
        }
        return sinLoQueQuedoPegado(tramo);
    }

    /**
     * Lo que queda pegado al nombre cuando el tramo trae dos códigos postales.
     *
     * "Av. Pte. J. D. Perón, B1663EDH 1390San Miguel": el código largo y el viejo de
     * cuatro números, y el viejo sin un espacio que lo separe de la localidad. Sacar el
     * primero dejaba "1390San Miguel", que es como esa zona aparecía en el selector.
     *
     * Es distinto del caso que ya cubría {@link #esLoQueQuedaDeUnCodigoPostal}: ahí el
     * resto queda solo y se lo reconoce entero, acá viene pegado al nombre y hay que
     * encontrar dónde termina.
     *
     * El corte pide dos cosas para no partir una localidad al medio: que lo pegado sea
     * código —números, o hasta tres mayúsculas, que es la cola del código argentino— y
     * que lo que sigue arranque un nombre, con mayúscula y después minúscula. Que esté
     * pegado es lo que salva a las localidades que empiezan con una palabra corta: en
     * "GBA Sur" o "La Plata" el nombre no está pegado, está al lado.
     */
    private static String sinLoQueQuedoPegado(String localidad) {
        return RESTO_PEGADO_AL_NOMBRE.matcher(localidad).replaceFirst("");
    }

    private static final Pattern RESTO_PEGADO_AL_NOMBRE =
        Pattern.compile("^(?:[A-Z0-9]*[0-9][A-Z0-9]*|[A-Z]{1,3})(?=\\p{Lu}\\p{Ll})");

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
