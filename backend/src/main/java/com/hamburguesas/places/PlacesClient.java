package com.hamburguesas.places;

import tools.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Thin wrapper over Google Places API (New).
 *
 * The field mask below is what keeps every search on the Pro SKU (5.000 free calls a month);
 * asking for ratings, opening hours or reviews would move it to the Enterprise SKU, which has
 * a far smaller free allowance.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PlacesClient {

    private static final String SEARCH_URL = "https://places.googleapis.com/v1/places:searchText";

    /**
     * El tipo de Google para hamburgueserías. Buscar solo por texto traía bares y
     * parrillas que mencionan hamburguesas —en la base quedaron un wine bar y dos
     * bares—, y con esto Google filtra por lo que el local es, no por lo que dice.
     */
    private static final String BURGER_TYPE = "hamburger_restaurant";
    private static final String FIELD_MASK =
        "places.id,places.displayName,places.formattedAddress,places.location,places.photos,nextPageToken";

    private final PlacesProperties properties;

    /**
     * Sigue redirecciones, que es lo que hace falta para bajar una foto: el endpoint
     * de Google no devuelve la imagen sino un 302 hacia ella, con un JSON en el cuerpo.
     * El cliente por omisión no las sigue, así que guardábamos ese JSON como si fuera
     * la foto: 345 archivos de 700 bytes que el navegador mostraba rotos.
     */
    private final RestClient restClient = RestClient.builder()
        .requestFactory(new JdkClientHttpRequestFactory(
            HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()))
        .build();

    public PlacesSearchResult searchText(String query, String pageToken) {
        Map<String, Object> body = new HashMap<>();
        body.put("textQuery", query);
        body.put("languageCode", "es");
        body.put("includedType", BURGER_TYPE);
        // Sin esto el tipo es apenas una preferencia y Google igual devuelve otros rubros.
        body.put("strictTypeFiltering", true);
        if (pageToken != null && !pageToken.isBlank()) {
            body.put("pageToken", pageToken);
        }

        JsonNode response = restClient.post()
            .uri(SEARCH_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Goog-Api-Key", properties.getApiKey())
            .header("X-Goog-FieldMask", FIELD_MASK)
            .body(body)
            .retrieve()
            .body(JsonNode.class);

        return parse(response);
    }

    public byte[] downloadPhoto(String photoName) {
        String uri = "https://places.googleapis.com/v1/%s/media?maxWidthPx=%d&key=%s"
            .formatted(photoName, properties.getPhotos().getMaxWidthPx(), properties.getApiKey());

        return restClient.get()
            .uri(uri)
            .retrieve()
            .body(byte[].class);
    }

    // Package-private y estático para poder probarlo sin salir a la red: es la
    // parte de este archivo que la migración a Jackson 3 podía romper en silencio.
    static PlacesSearchResult parse(JsonNode response) {
        List<PlacesSearchResult.Place> places = new ArrayList<>();
        if (response == null) {
            return new PlacesSearchResult(places, null);
        }

        for (JsonNode node : response.path("places")) {
            places.add(new PlacesSearchResult.Place(
                node.path("id").asText(null),
                node.path("displayName").path("text").asText(null),
                node.path("formattedAddress").asText(null),
                node.path("location").path("latitude").isMissingNode()
                    ? null : node.path("location").path("latitude").asDouble(),
                node.path("location").path("longitude").isMissingNode()
                    ? null : node.path("location").path("longitude").asDouble(),
                bestPhotoName(node)
            ));
        }

        return new PlacesSearchResult(places, response.path("nextPageToken").asText(null));
    }

    /**
     * Elige una foto entre las que devuelve Google, que llegan sin ninguna etiqueta
     * de qué muestran: no hay forma de pedirle "el logo" o "la fachada".
     *
     * Lo que sí viene es quién subió cada una. Las del propio local —las que carga el
     * dueño en su ficha— son casi siempre el logo o el frente, mientras que las de los
     * clientes suelen ser platos y mesas. Así que se prefiere la del local y, si no
     * subió ninguna, queda la primera, que es la que Google muestra como principal.
     */
    private static String bestPhotoName(JsonNode place) {
        JsonNode photos = place.path("photos");
        if (!photos.isArray() || photos.isEmpty()) {
            return null;
        }

        String placeName = place.path("displayName").path("text").asText("");
        for (JsonNode photo : photos) {
            for (JsonNode author : photo.path("authorAttributions")) {
                if (!placeName.isBlank()
                    && placeName.equalsIgnoreCase(author.path("displayName").asText(""))) {
                    return photo.path("name").asText(null);
                }
            }
        }

        return photos.get(0).path("name").asText(null);
    }
}
