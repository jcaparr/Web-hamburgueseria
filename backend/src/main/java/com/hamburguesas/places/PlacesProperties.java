package com.hamburguesas.places;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Data
@ConfigurationProperties(prefix = "app.places")
public class PlacesProperties {

    /** Read from the GOOGLE_MAPS_API_KEY environment variable. Empty disables every Google call. */
    private String apiKey = "";

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
        private int maxPagesPerArea = 2;
        private long delayBetweenCallsMs = 500;
    }

    @Data
    public static class Quota {
        /** Kept under Google's 5.000 free monthly Text Search calls. */
        private int monthlySearchCalls = 4000;
        /** El tramo gratuito de Google es de 1.000 fotos por mes, y acá se frena antes. */
        private int monthlyPhotoCalls = 1000;
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
