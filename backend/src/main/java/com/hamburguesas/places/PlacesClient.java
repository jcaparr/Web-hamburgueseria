package com.hamburguesas.places;

import tools.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

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
    private static final String FIELD_MASK =
        "places.id,places.displayName,places.formattedAddress,places.location,places.photos,nextPageToken";

    private final PlacesProperties properties;
    private final RestClient restClient = RestClient.create();

    public PlacesSearchResult searchText(String query, String pageToken) {
        Map<String, Object> body = new HashMap<>();
        body.put("textQuery", query);
        body.put("languageCode", "es");
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
                node.path("photos").isArray() && !node.path("photos").isEmpty()
                    ? node.path("photos").get(0).path("name").asText(null) : null
            ));
        }

        return new PlacesSearchResult(places, response.path("nextPageToken").asText(null));
    }
}
