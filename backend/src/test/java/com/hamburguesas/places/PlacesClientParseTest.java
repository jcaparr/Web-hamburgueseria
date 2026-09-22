package com.hamburguesas.places;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cubre el parseo de la respuesta de Google Places.
 *
 * Existe por la migración a Spring Boot 4: Jackson pasó de com.fasterxml a
 * tools.jackson, y este es el único lugar del proyecto que manipula un JsonNode
 * a mano. El resto del código no toca Jackson directamente, así que un cambio de
 * comportamiento en path()/asText() no lo notaría nadie hasta la próxima
 * sincronización —que corre por cron, de madrugada y contra la API paga.
 *
 * No sale a la red: le damos el JSON que Google devuelve y miramos qué sale.
 */
class PlacesClientParseTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private static JsonNode json(String raw) {
        return MAPPER.readTree(raw);
    }

    @Test
    void leeLosCamposDeUnLugarCompleto() {
        JsonNode response = json("""
            {
              "places": [{
                "id": "ChIJ123",
                "displayName": { "text": "Thunder Burger", "languageCode": "es" },
                "formattedAddress": "Costa Rica 5827, CABA",
                "location": { "latitude": -34.5806965, "longitude": -58.4368432 },
                "photos": [{ "name": "places/ChIJ123/photos/abc" }]
              }],
              "nextPageToken": "token-siguiente"
            }
            """);

        PlacesSearchResult result = PlacesClient.parse(response);

        assertThat(result.nextPageToken()).isEqualTo("token-siguiente");
        assertThat(result.places()).hasSize(1);

        PlacesSearchResult.Place place = result.places().get(0);
        assertThat(place.placeId()).isEqualTo("ChIJ123");
        // El nombre está anidado: displayName.text, no displayName.
        assertThat(place.name()).isEqualTo("Thunder Burger");
        assertThat(place.address()).isEqualTo("Costa Rica 5827, CABA");
        assertThat(place.latitude()).isEqualTo(-34.5806965);
        assertThat(place.longitude()).isEqualTo(-58.4368432);
        assertThat(place.photoName()).isEqualTo("places/ChIJ123/photos/abc");
    }

    /**
     * El caso que de verdad importa: Places omite los campos que no tiene en vez
     * de mandarlos en null. Si un campo ausente volviera como el string "null" en
     * lugar de null —que es justo el tipo de detalle que cambia entre versiones de
     * Jackson— guardaríamos basura en la base sin que fallara nada.
     */
    /**
     * Google no dice qué muestra cada foto, así que la única señal disponible es quién
     * la subió: las del propio local suelen ser el logo o el frente, y las de los
     * clientes, los platos. En la prueba real sobre Palermo, 14 de 20 locales tenían
     * una foto propia.
     */
    @Test
    void prefiereLaFotoQueSubioElLocal() {
        JsonNode response = json("""
            {
              "places": [{
                "id": "ChIJ123",
                "displayName": { "text": "Thunder Burger" },
                "formattedAddress": "Costa Rica 5827",
                "photos": [
                  {
                    "name": "places/ChIJ123/photos/de-un-cliente",
                    "authorAttributions": [{ "displayName": "Andrea Mansilla" }]
                  },
                  {
                    "name": "places/ChIJ123/photos/del-local",
                    "authorAttributions": [{ "displayName": "Thunder Burger" }]
                  }
                ]
              }]
            }
            """);

        PlacesSearchResult.Place place = PlacesClient.parse(response).places().get(0);

        assertThat(place.photoName()).isEqualTo("places/ChIJ123/photos/del-local");
    }

    /** Si el local no subió ninguna queda la primera, que es la que Google destaca. */
    @Test
    void sinFotoDelLocalSeQuedaConLaPrimera() {
        JsonNode response = json("""
            {
              "places": [{
                "id": "ChIJ123",
                "displayName": { "text": "Thunder Burger" },
                "formattedAddress": "Costa Rica 5827",
                "photos": [
                  {
                    "name": "places/ChIJ123/photos/primera",
                    "authorAttributions": [{ "displayName": "Andrea Mansilla" }]
                  },
                  {
                    "name": "places/ChIJ123/photos/segunda",
                    "authorAttributions": [{ "displayName": "Micaela Rodríguez" }]
                  }
                ]
              }]
            }
            """);

        PlacesSearchResult.Place place = PlacesClient.parse(response).places().get(0);

        assertThat(place.photoName()).isEqualTo("places/ChIJ123/photos/primera");
    }

    @Test
    void unLugarSinFotoNiUbicacionNoInventaValores() {
        JsonNode response = json("""
            {
              "places": [{
                "id": "ChIJ456",
                "displayName": { "text": "Sin Datos" },
                "formattedAddress": "Alguna dirección"
              }]
            }
            """);

        PlacesSearchResult.Place place = PlacesClient.parse(response).places().get(0);

        assertThat(place.latitude()).isNull();
        assertThat(place.longitude()).isNull();
        assertThat(place.photoName()).isNull();
    }

    @Test
    void sinNextPageTokenDevuelveNullYNoElStringNull() {
        JsonNode response = json("""
            { "places": [] }
            """);

        PlacesSearchResult result = PlacesClient.parse(response);

        assertThat(result.places()).isEmpty();
        assertThat(result.nextPageToken()).isNull();
    }

    /** Una respuesta vacía del cliente HTTP no debe romper la sincronización. */
    @Test
    void respuestaNulaNoExplota() {
        PlacesSearchResult result = PlacesClient.parse(null);

        assertThat(result.places()).isEmpty();
        assertThat(result.nextPageToken()).isNull();
    }
}
