package com.hamburguesas.places;

import tools.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.text.Normalizer;
import java.util.Locale;

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

    /**
     * Qué versión de la regla de elección de foto es esta. Se guarda junto a cada foto
     * bajada, y cuando el número sube, las fotos elegidas con la regla anterior se
     * revisan una vez. Sin esto una mejora en la regla solo alcanzaría a los locales
     * nuevos, y los 424 que ya tienen foto se quedarían con la elección vieja.
     *
     * 1: la primera del local, o la primera de todas.
     * 2: la del local, prefiriendo apaisadas.
     * 3: además, descarta las capturas de pantalla.
     * 4: manda la forma sobre quién la subió, porque el local sube su marca.
     */
    public static final int REGLA_DE_FOTO = 4;
    private static final String FIELD_MASK =
        "places.id,places.displayName,places.formattedAddress,places.location,places.photos,"
        + "places.primaryType,nextPageToken";

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

    /**
      * Pide la ficha de un local puntual. Existe para los locales que ninguna búsqueda
      * por barrio devuelve —los que Google no clasifica como hamburguesería, como
      * Burger King—, que si no se quedarían sin foto para siempre.
      *
      * @return el nombre de la foto elegida, o null si el local no tiene ninguna.
      */
     public String photoNameFor(String placeId) {
         JsonNode place = restClient.get()
             .uri("https://places.googleapis.com/v1/places/{placeId}", placeId)
             .header("X-Goog-Api-Key", properties.getApiKey())
             .header("X-Goog-FieldMask", "id,displayName,photos")
             .retrieve()
             .body(JsonNode.class);

         return place == null ? null : bestPhotoName(place);
     }

    /**
     * La clave va en el header, igual que en las otras dos llamadas.
     *
     * Antes viajaba en la query string, y de ahí saltaba al log: el único error que se
     * atrapa al bajar una foto es el de respuesta HTTP, así que un corte de red tira una
     * ResourceAccessException que nadie agarra, y su mensaje es
     * "I/O error on GET request for <URI completa>". Un timeout alcanzaba para dejar la
     * clave escrita en los logs del servidor.
     *
     * El nombre de la foto se arma aparte y no como plantilla de URI: trae barras
     * adentro —"places/ChIJ.../photos/AXQ..."— y expandirlo las escaparía a %2F,
     * dejando una ruta que Google no reconoce.
     */
    public byte[] downloadPhoto(String photoName) {
        URI uri = URI.create("https://places.googleapis.com/v1/%s/media?maxWidthPx=%d"
            .formatted(photoName, properties.getPhotos().getMaxWidthPx()));

        return restClient.get()
            .uri(uri)
            .header("X-Goog-Api-Key", properties.getApiKey())
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
                bestPhotoName(node),
                node.path("primaryType").asText(null)
            ));
        }

        return new PlacesSearchResult(places, response.path("nextPageToken").asText(null));
    }

    /**
     * Elige una foto entre las que devuelve Google, que llegan sin ninguna etiqueta
     * de qué muestran: no hay forma de pedirle "el logo" o "la fachada".
     *
     * Lo que se busca es una hamburguesa que se vea bien, y si no, el local bien
     * fotografiado. Nada de eso viene dicho, así que se deduce de lo único que Google
     * cuenta de cada foto: quién la subió y qué tamaño tiene.
     *
     * Quién la subió pesaba más que todo lo demás, y resultó ser la señal equivocada.
     * El local sube su marca: de las diez fotos de "Keke & Larry", las dos suyas son el
     * logo y una promoción de empanadas, y la hamburguesa con papas la sacó un cliente.
     * Es lo razonable: al dueño le importa la identidad del local, y al que fue a comer
     * le importa el plato. Así que ahora la foto del local es un desempate y no una
     * garantía: una apaisada grande de un cliente le gana.
     */
    private static String bestPhotoName(JsonNode place) {
        JsonNode photos = place.path("photos");
        if (!photos.isArray() || photos.isEmpty()) {
            return null;
        }

        String placeName = place.path("displayName").path("text").asText("");

        JsonNode elegida = null;
        int mejorPuntaje = Integer.MIN_VALUE;
        for (JsonNode photo : photos) {
            int puntaje = puntajeDe(photo, placeName);
            if (puntaje > mejorPuntaje) {
                mejorPuntaje = puntaje;
                elegida = photo;
            }
        }

        return elegida == null ? null : elegida.path("name").asText(null);
    }

    /**
     * Qué tan buena es una foto como portada del local. Mayor es mejor, y ante empate
     * gana la primera, que es la que Google muestra como principal.
     *
     * Manda la forma de la foto por sobre quién la subió. Una apaisada grande de un
     * cliente le gana a una vertical del local, porque lo que el local sube suele ser
     * su marca y lo que sube el cliente suele ser lo que comió.
     */
    private static int puntajeDe(JsonNode photo, String placeName) {
        int puntaje = 0;

        // Alcanza para desempatar entre dos fotos de la misma forma, y no para que una
        // vertical del local le gane a una apaisada de un cliente.
        if (laSubioElLocal(photo, placeName)) {
            puntaje += 15;
        }

        // Las tarjetas recortan la imagen a 4:3, así que una foto vertical —el plato
        // que saca un cliente desde arriba— queda recortada al centro y se pierde el
        // local. Las fachadas y las portadas que sube el dueño suelen ser apaisadas.
        double proporcion = proporcionDe(photo);
        if (proporcion >= 1.2) {
            puntaje += 20;
        } else if (proporcion > 0 && proporcion < CAPTURA_DE_PANTALLA) {
            // Más alta que el doble de su ancho no es una foto sacada con la cámara:
            // es una captura de pantalla. "Valentino Burger" tenía de portada una
            // captura de una historia de Instagram —1080x2400, con el nombre de quien
            // la publicó arriba y el "Enviar mensaje" abajo—, y como la había subido el
            // propio local se quedaba con la portada. El castigo alcanza para que
            // pierda contra cualquier foto apaisada, incluso la de un cliente.
            puntaje -= 150;
        } else if (proporcion <= 0.85) {
            puntaje -= 20;
        }

        // Entre dos parecidas, la más grande: las chicas suelen ser logos recortados o
        // capturas, y encima se ven mal estiradas en la portada del detalle. Los dos
        // escalones son para que el tamaño desempate sin dar vuelta la forma.
        int ancho = photo.path("widthPx").asInt(0);
        if (ancho >= 2000) {
            puntaje += 6;
        } else if (ancho >= 1000) {
            puntaje += 3;
        }

        return puntaje;
    }

    /**
     * Debajo de esta proporción la imagen es más alta que el doble de su ancho. Ninguna
     * cámara de celular saca así —las más estiradas dan 9:16, o sea 0,56—, con lo cual
     * lo que hay ahí es la pantalla entera de un teléfono capturada.
     */
    private static final double CAPTURA_DE_PANTALLA = 0.5;

    private static double proporcionDe(JsonNode photo) {
        int alto = photo.path("heightPx").asInt(0);
        return alto == 0 ? 0 : (double) photo.path("widthPx").asInt(0) / alto;
    }

    /**
     * Si la foto la subió el propio local.
     *
     * No se pide igualdad exacta porque casi nunca la hay: el local figura en Google
     * como "Valentino" y en el mapa como "Valentino Burger", o al revés, "Mi Barrio
     * Hamburguesería" sube las fotos de "Mi Barrio Hamburguesería Caballito". Con
     * igualdad exacta se perdían 2 de cada 10 fotos oficiales.
     *
     * Alcanza con que un nombre contenga al otro, ignorando acentos, mayúsculas y
     * puntuación. Se exige un mínimo de 4 caracteres para que un local de nombre muy
     * corto no se quede con la foto de cualquier persona que se llame parecido.
     */
    private static boolean laSubioElLocal(JsonNode photo, String placeName) {
        String local = soloLetrasYNumeros(placeName);
        if (local.length() < 4) {
            return false;
        }

        for (JsonNode author : photo.path("authorAttributions")) {
            String autor = soloLetrasYNumeros(author.path("displayName").asText(""));
            if (autor.length() >= 4 && (autor.contains(local) || local.contains(autor))) {
                return true;
            }
        }
        return false;
    }

    private static String soloLetrasYNumeros(String valor) {
        return Normalizer.normalize(valor, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]", "");
    }
}
