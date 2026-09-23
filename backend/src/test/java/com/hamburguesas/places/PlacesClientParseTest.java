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

    /**
     * Caso real: el local figura como "Valentino Burger" y sus fotos las subió
     * "Valentino". Con igualdad exacta se perdían 2 de cada 10 fotos oficiales.
     */
    @Test
    void reconoceLaFotoDelLocalAunqueElNombreNoSeaIdentico() {
        JsonNode response = json("""
            {
              "places": [{
                "id": "ChIJ123",
                "displayName": { "text": "Valentino Burger" },
                "formattedAddress": "Puán 1406",
                "photos": [
                  {
                    "name": "places/ChIJ123/photos/de-un-cliente",
                    "widthPx": 4000, "heightPx": 2252,
                    "authorAttributions": [{ "displayName": "Andrea Mansilla" }]
                  },
                  {
                    "name": "places/ChIJ123/photos/del-local",
                    "widthPx": 1280, "heightPx": 720,
                    "authorAttributions": [{ "displayName": "Valentino" }]
                  }
                ]
              }]
            }
            """);

        PlacesSearchResult.Place place = PlacesClient.parse(response).places().get(0);

        assertThat(place.photoName()).isEqualTo("places/ChIJ123/photos/del-local");
    }

    /**
     * Las tarjetas recortan a 4:3: una foto vertical de un plato queda recortada al
     * centro y se pierde el local. Entre varias del propio local, gana la apaisada.
     */
    @Test
    void entreVariasDelLocalPrefiereLaApaisada() {
        JsonNode response = json("""
            {
              "places": [{
                "id": "ChIJ123",
                "displayName": { "text": "El Desembarco Caballito" },
                "formattedAddress": "Acoyte 100",
                "photos": [
                  {
                    "name": "places/ChIJ123/photos/vertical",
                    "widthPx": 1290, "heightPx": 2266,
                    "authorAttributions": [{ "displayName": "El Desembarco Caballito" }]
                  },
                  {
                    "name": "places/ChIJ123/photos/apaisada",
                    "widthPx": 3464, "heightPx": 2309,
                    "authorAttributions": [{ "displayName": "El Desembarco Caballito" }]
                  }
                ]
              }]
            }
            """);

        PlacesSearchResult.Place place = PlacesClient.parse(response).places().get(0);

        assertThat(place.photoName()).isEqualTo("places/ChIJ123/photos/apaisada");
    }

    /** Una foto del local, aunque sea vertical, vale más que una apaisada de un cliente. */
    @Test
    void laDelLocalGanaAunqueSeaVerticalYLaDelClienteApaisada() {
        JsonNode response = json("""
            {
              "places": [{
                "id": "ChIJ123",
                "displayName": { "text": "Burger Couple" },
                "formattedAddress": "Rivadavia 1",
                "photos": [
                  {
                    "name": "places/ChIJ123/photos/cliente-apaisada",
                    "widthPx": 4032, "heightPx": 3024,
                    "authorAttributions": [{ "displayName": "Enecehache Enecehache" }]
                  },
                  {
                    "name": "places/ChIJ123/photos/local-vertical",
                    "widthPx": 1290, "heightPx": 2266,
                    "authorAttributions": [{ "displayName": "Burger Couple" }]
                  }
                ]
              }]
            }
            """);

        PlacesSearchResult.Place place = PlacesClient.parse(response).places().get(0);

        assertThat(place.photoName()).isEqualTo("places/ChIJ123/photos/local-vertical");
    }

    /**
     * Una captura de pantalla pierde aunque la haya subido el local.
     *
     * "Valentino Burger" tenía de portada una captura de una historia de Instagram
     * —1080x2400, con el nombre de quien la publicó arriba y el "Enviar mensaje"
     * abajo—, porque la había subido el local y eso pesaba más que todo lo demás. Una
     * imagen más alta que el doble de su ancho no la sacó ninguna cámara.
     */
    @Test
    void unaCapturaDePantallaDelLocalPierdeContraLaFotoDeUnCliente() {
        JsonNode response = json("""
            {
              "places": [{
                "id": "ChIJ123",
                "displayName": { "text": "Valentino Burger" },
                "formattedAddress": "Puán 380",
                "photos": [
                  {
                    "name": "places/ChIJ123/photos/captura-del-local",
                    "widthPx": 1080, "heightPx": 2400,
                    "authorAttributions": [{ "displayName": "Valentino" }]
                  },
                  {
                    "name": "places/ChIJ123/photos/cliente-apaisada",
                    "widthPx": 4080, "heightPx": 1836,
                    "authorAttributions": [{ "displayName": "Lucas Lobo" }]
                  }
                ]
              }]
            }
            """);

        PlacesSearchResult.Place place = PlacesClient.parse(response).places().get(0);

        assertThat(place.photoName()).isEqualTo("places/ChIJ123/photos/cliente-apaisada");
    }

    /** Sin foto del local, entre las de clientes gana la apaisada sobre la vertical. */
    @Test
    void sinFotoDelLocalPrefiereLaApaisadaDeUnCliente() {
        JsonNode response = json("""
            {
              "places": [{
                "id": "ChIJ123",
                "displayName": { "text": "Mr Tasty Caballito" },
                "formattedAddress": "Rivadavia 2",
                "photos": [
                  {
                    "name": "places/ChIJ123/photos/vertical",
                    "widthPx": 3024, "heightPx": 4032,
                    "authorAttributions": [{ "displayName": "Pablo José Santos" }]
                  },
                  {
                    "name": "places/ChIJ123/photos/apaisada",
                    "widthPx": 4080, "heightPx": 3060,
                    "authorAttributions": [{ "displayName": "Analía Lara" }]
                  }
                ]
              }]
            }
            """);

        PlacesSearchResult.Place place = PlacesClient.parse(response).places().get(0);

        assertThat(place.photoName()).isEqualTo("places/ChIJ123/photos/apaisada");
    }

    /**
     * Un nombre muy corto no puede quedarse con la foto de cualquiera que se llame
     * parecido: hay locales que se llaman "Rubi" o "Heaven".
     */
    @Test
    void unNombreCortoNoSeQuedaConLaFotoDeUnaPersonaParecida() {
        JsonNode response = json("""
            {
              "places": [{
                "id": "ChIJ123",
                "displayName": { "text": "Rubi" },
                "formattedAddress": "Alsina 3",
                "photos": [
                  {
                    "name": "places/ChIJ123/photos/primera",
                    "widthPx": 4000, "heightPx": 3000,
                    "authorAttributions": [{ "displayName": "Rubi Fernández" }]
                  }
                ]
              }]
            }
            """);

        PlacesSearchResult.Place place = PlacesClient.parse(response).places().get(0);

        // Igual queda esta porque es la única, pero no por creerla oficial.
        assertThat(place.photoName()).isEqualTo("places/ChIJ123/photos/primera");
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
