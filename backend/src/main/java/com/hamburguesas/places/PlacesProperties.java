package com.hamburguesas.places;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Data
@ConfigurationProperties(prefix = "app.places")
public class PlacesProperties {

    /** Read from the GOOGLE_MAPS_API_KEY environment variable. Empty disables every Google call. */
    private String apiKey = "";

    /**
     * Las cadenas de comida rápida, para poder sacarlas del listado. Se reconocen por
     * el principio del nombre, sin mayúsculas ni puntuación: "burgerking" abarca a
     * "Burger King - Sucursal P.Italia".
     */
    private List<String> fastFoodBrands = List.of();

    private Sync sync = new Sync();
    private Quota quota = new Quota();
    private Photos photos = new Photos();

    @Data
    public static class Sync {
        private boolean enabled = false;
        private String cron = "0 0 4 * * SUN";
        /** Shared secret required by the manual trigger endpoint. Empty disables the endpoint. */
        private String triggerToken = "";
        private List<String> areas = List.of();

        /**
         * Las formas de preguntar por un barrio. Cada una es una búsqueda aparte con su
         * propio tope de 60 resultados, y Google contesta distinto según cómo se le
         * pregunte, así que sumarlas es lo que amplía la cobertura. El {barrio} se
         * reemplaza por cada uno de los de arriba.
         */
        private List<String> queryTemplates = List.of("hamburguesería en {barrio}, Buenos Aires");

        /**
         * Rubros que no son un lugar donde comer una hamburguesa. Google igual los
         * devuelve con el tipo hamburguesería encima: una fábrica de medallones, un
         * mayorista, una carnicería y un pelotero estaban en la base como locales.
         */
        private List<String> excludedPrimaryTypes = List.of();

        /**
         * Locales sueltos que Google clasifica mal y ninguna regla puede filtrar.
         * Se anotan acá por su identificador, con el motivo al lado en la configuración.
         */
        private List<String> excludedPlaceIds = List.of();

        /**
         * Hamburgueserías que Google clasifica como otra cosa y que ninguna regla
         * rescata: "Beggars" figura como bar y "Draken" como cervecería. Van con el
         * motivo anotado al lado en la configuración.
         */
        private List<String> includedPlaceIds = List.of();
        private int maxPagesPerArea = 2;
        private long delayBetweenCallsMs = 500;
    }

    @Data
    public static class Quota {
        /** Kept under Google's 5.000 free monthly Text Search calls. */
        private int monthlySearchCalls = 4000;
        /** El tramo gratuito de Google es de 1.000 fotos por mes, y acá se frena antes. */
        private int monthlyPhotoCalls = 1000;
        /**
         * El resumen de reseñas, que es el tramo más caro: mil gratis por mes y
         * veinticinco dólares cada mil después. Se frena antes a propósito.
         */
        private int monthlyResumenCalls = 900;
        /** Fichas sueltas, para los locales que ninguna búsqueda devuelve. Gratis hasta 5.000. */
        private int monthlyDetailsCalls = 4000;
    }

    @Data
    public static class Photos {
        private String directory = "./data/place-photos";
        private int maxWidthPx = 800;
    }

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}
