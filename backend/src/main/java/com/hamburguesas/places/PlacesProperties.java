package com.hamburguesas.places;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "app.places")
public class PlacesProperties {

    /** Read from the GOOGLE_MAPS_API_KEY environment variable. Empty disables every Google call. */
    private String apiKey = "";

    /**
     * Las cadenas de comida rápida, que no aparecen en Explorar salvo buscándolas por
     * nombre (#206). Se reconocen por el principio del nombre, sin mayúsculas ni
     * puntuación: "burgerking" abarca a "Burger King - Sucursal P.Italia".
     */
    private List<String> fastFoodBrands = List.of();

    /**
     * Cómo se escribe cada cadena de {@link #fastFoodBrands}, para su tarjeta y su
     * página: la clave es la misma de esa lista, "mcdonalds", y el valor "McDonald's".
     *
     * Escrito a mano y no sacado de las sucursales porque cada una se llama distinto
     * —"McDonald's Abasto Patio de Comidas", "BURGER KING"— y no hay una que sirva de
     * nombre para todas. Un test pide que no falte ninguna.
     */
    private Map<String, String> nombresDeCadenas = Map.of();

    /**
     * Marcas con varias sucursales que no son cadenas de comida rápida.
     *
     * Están aparte de {@link #fastFoodBrands} porque las dos listas contestan preguntas
     * distintas. Esa dice qué se esconde cuando alguien apaga las cadenas en Explorar;
     * esta dice nada más qué sucursales pueden compartir una portada.
     *
     * Mezclarlas tenía un costo concreto: para prestarle la foto a una sucursal de La
     * Birra había que declararla comida rápida, y entonces desaparecía del listado de
     * quien apaga las cadenas. Son justo los lugares que esa persona quiere ver.
     */
    private List<String> marcasConSucursales = List.of();

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
         * Hasta dónde se busca, en kilómetros desde el Obelisco.
         *
         * Con 75 entran La Plata, San Vicente, Cañuelas, Luján y Belén de Escobar, que
         * son los bordes que se quisieron cubrir.
         */
        private double radioEnKm = 75;

        /**
         * Las formas de preguntar por un barrio. Cada una es una búsqueda aparte con su
         * propio tope de 60 resultados, y Google contesta distinto según cómo se le
         * pregunte, así que sumarlas es lo que amplía la cobertura. El {barrio} se
         * reemplaza por cada uno de los de arriba, con ", Buenos Aires" detrás, o por la
         * zona que se pida barrir sola, tal cual se la escribe (#222).
         */
        private List<String> queryTemplates = List.of("hamburguesería en {barrio}");

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

        /**
         * La foto de portada elegida a mano, para los locales donde la regla no acierta.
         *
         * La regla sabe elegir una foto bien sacada —apaisada, grande, no una captura de
         * pantalla—, pero no sabe qué hay adentro: entre la fachada del local y una
         * bandeja de empanadas no puede distinguir, porque las dos son fotografías. Eso
         * solo se arregla mirando, y lo que se mira una vez conviene que quede escrito.
         *
         * La clave es el place_id y el valor la huella de la foto: "1600x1200|Quien la
         * subió". Se usa la huella y no el nombre porque el nombre que devuelve Google es
         * un vale de descarga que cambia en cada pedido; la huella es lo único que vuelve
         * igual.
         *
         * Si la huella anotada no aparece entre las fotos del local —porque la borraron,
         * o porque se copió mal— no pasa nada: manda la regla, como si no estuviera
         * anotada. Vale la pena saberlo al revisar por qué un local no cambió la foto.
         */
        private Map<String, String> fotosElegidas = Map.of();

        /**
         * Cuántas fotos tiene que tener en Google un local que Google NO clasifica como
         * hamburguesería para que lo dejemos en la lista.
         *
         * Las dos condiciones juntas, nunca sueltas. Que Google lo llame "restaurant" no
         * alcanza —La Birra Bar figura como bar y es de las mejores— y tener pocas fotos
         * tampoco: una hamburguesería de barrio recién abierta puede tener una sola, y
         * sacarla sería castigar al conurbano por ser menos fotografiado. Es el cruce lo
         * que señala al kiosco que vende hamburguesas sueltas.
         *
         * Cero apaga la regla.
         */
        private int fotosMinimasSiNoEsHamburgueseria = 10;

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
        /**
         * El horario de apertura, que es un campo Enterprise: mil gratis por mes, aparte de
         * las fichas. Con mil y pico de locales no alcanza para todos en un mes, y está
         * bien: el resto lo completa el mes siguiente.
         */
        private int monthlyHorarioCalls = 950;
        /**
         * Las fichas que se piden para saber qué fotos tiene un local. Google no las
         * cobra, así que el tope no es por plata: es el tramo gratuito de las fichas Pro,
         * por si alguna vez se le agrega un campo pago a la consulta sin darse cuenta.
         */
        private int monthlyListaDeFotosCalls = 5000;
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
