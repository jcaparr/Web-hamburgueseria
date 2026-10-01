package com.hamburguesas.places;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Thin wrapper over Google Places API (New).
 *
 * The field mask below is what keeps every search on the Pro SKU (5.000 free calls a month);
 * asking for ratings, opening hours or reviews would move it to the Enterprise SKU, which has
 * a far smaller free allowance.
 *
 * Solo sabe cómo preguntar. Qué foto conviene de las que vuelven lo decide
 * {@link EleccionDeFoto}.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PlacesClient {

    private static final String SEARCH_URL = "https://places.googleapis.com/v1/places:searchText";
    private static final String PLACE_URL = "https://places.googleapis.com/v1/places/{placeId}";

    /**
     * El tipo de Google para hamburgueserías. Buscar solo por texto traía bares y
     * parrillas que mencionan hamburguesas —en la base quedaron un wine bar y dos
     * bares—, y con esto Google filtra por lo que el local es, no por lo que dice.
     */
    private static final String BURGER_TYPE = "hamburger_restaurant";

    private static final String FIELD_MASK =
        "places.id,places.displayName,places.formattedAddress,places.location,places.photos,"
        + "places.primaryType,places.types,nextPageToken";

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
        return searchText(query, pageToken, true);
    }

    /**
     * Busca por texto, con la opción de no exigir el rubro de hamburguesería.
     *
     * El barrido lo exige siempre: recorre barrios con consultas genéricas y sin el
     * filtro estricto Google devuelve pizzerías, bares y cualquier cosa que le parezca
     * cerca. Ahí el filtro es lo que hace que la lista sirva.
     *
     * Pero para buscar un local puntual por su nombre, el mismo filtro lo vuelve
     * inservible: los locales que hay que agregar a mano son justamente los que Google
     * no clasifica como hamburguesería. Buscando "Austin's Diner & Grill" con el filtro
     * puesto, Google contesta con otro restaurante de Palermo, y el agujero que esto
     * viene a tapar se tapa a sí mismo.
     *
     * @param soloHamburgueserias false para buscar por nombre sin exigir el rubro
     */
    public PlacesSearchResult searchText(String query, String pageToken,
                                         boolean soloHamburgueserias) {
        Map<String, Object> body = new HashMap<>();
        body.put("textQuery", query);
        body.put("languageCode", "es");
        if (soloHamburgueserias) {
            body.put("includedType", BURGER_TYPE);
            // Sin esto el tipo es apenas una preferencia y Google igual devuelve otros rubros.
            body.put("strictTypeFiltering", true);
        }
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

        return parse(response, properties.getSync().getFotosElegidas());
    }

    /**
     * El resumen de reseñas que arma Google, o null si no tiene.
     *
     * Es lo que la gente que fue dice que comió, y resuelve los locales que ninguna otra
     * señal alcanza: "Austin's Diner & Grill" no tiene el rubro de hamburguesas ni lo
     * dice en el nombre, y su resumen habla de "delicious grilled and smash burgers".
     *
     * Es el tramo más caro de la API —Enterprise + Atmosphere, mil gratis por mes—, así
     * que se pide de a uno y solo por los locales que las pruebas baratas no resolvieron.
     *
     * Solo seis de cada diez locales tienen uno, así que su ausencia no prueba nada.
     */
    public String resumenDeResenias(String placeId) {
        JsonNode place = fichaDe(placeId, "reviewSummary");
        if (place == null) {
            return null;
        }
        return place.path("reviewSummary").path("text").path("text").asText(null);
    }

    /**
     * Las fotos de un local puntual, de la mejor a la peor según {@link EleccionDeFoto}.
     *
     * Existe para los locales que ninguna búsqueda por barrio devuelve —los que Google no
     * clasifica como hamburguesería, como Burger King—, que si no se quedarían sin foto
     * para siempre.
     *
     * @return todas las fotos que tiene, o una lista vacía si no tiene ninguna. El largo
     *         dice cuántas fotos tiene el local, y saberlo no cuesta nada más.
     */
    public List<FotoElegida> fotosDe(String placeId) {
        JsonNode place = fichaDe(placeId, "id,displayName,photos");
        return place == null ? List.of()
            : EleccionDeFoto.mejoresFotos(place, properties.getSync().getFotosElegidas().get(placeId));
    }

    /** La ficha de un local, con solo los campos que se piden: cada campo define el precio. */
    private JsonNode fichaDe(String placeId, String campos) {
        return restClient.get()
            .uri(PLACE_URL, placeId)
            .header("X-Goog-Api-Key", properties.getApiKey())
            .header("X-Goog-FieldMask", campos)
            .retrieve()
            .body(JsonNode.class);
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
        return parse(response, Map.of());
    }

    /**
     * Lo mismo, respetando las fotos elegidas a mano.
     *
     * También acá y no solo al revisar fotos: un local que entra por primera vez baja su
     * portada en este momento, y sin esto bajaría la de la regla para que después alguien
     * la cambie. Son dos descargas en lugar de una, y la cuota de fotos es el tramo
     * gratuito más chico que tenemos.
     */
    static PlacesSearchResult parse(JsonNode response, Map<String, String> fotosElegidas) {
        List<PlacesSearchResult.Place> places = new ArrayList<>();
        if (response == null) {
            return new PlacesSearchResult(places, null);
        }

        for (JsonNode node : response.path("places")) {
            List<FotoElegida> mejores =
                EleccionDeFoto.mejoresFotos(node, fotosElegidas.get(node.path("id").asText(null)));
            FotoElegida foto = mejores.isEmpty() ? null : mejores.get(0);
            places.add(new PlacesSearchResult.Place(
                node.path("id").asText(null),
                node.path("displayName").path("text").asText(null),
                node.path("formattedAddress").asText(null),
                node.path("location").path("latitude").isMissingNode()
                    ? null : node.path("location").path("latitude").asDouble(),
                node.path("location").path("longitude").isMissingNode()
                    ? null : node.path("location").path("longitude").asDouble(),
                foto == null ? null : foto.name(),
                foto == null ? null : foto.huella(),
                node.path("primaryType").asText(null),
                rubrosDe(node)
            ));
        }

        return new PlacesSearchResult(places, response.path("nextPageToken").asText(null));
    }

    /**
     * Todos los rubros que Google le pone al local.
     *
     * Vacío si no vinieron, que no es lo mismo que "ninguno": quien decide después trata
     * la lista vacía como falta de pruebas y no como prueba en contra.
     */
    private static Set<String> rubrosDe(JsonNode place) {
        Set<String> rubros = new HashSet<>();
        for (JsonNode rubro : place.path("types")) {
            String texto = rubro.asText(null);
            if (texto != null) {
                rubros.add(texto);
            }
        }
        return rubros;
    }
}
