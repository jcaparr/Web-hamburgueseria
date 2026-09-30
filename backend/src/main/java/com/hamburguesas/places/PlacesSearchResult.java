package com.hamburguesas.places;

import java.util.List;
import java.util.Set;

/** Flattened view of one page of Google Places Text Search results. */
public record PlacesSearchResult(
    List<Place> places,
    String nextPageToken
) {
    public record Place(
        String placeId,
        String name,
        String address,
        Double latitude,
        Double longitude,
        /** El vale para bajarla, que sirve una sola vez: Google lo cambia en cada pedido. */
        String photoName,
        /** Cómo reconocerla después. El porqué está en FotoElegida. */
        String photoFingerprint,
        /** El rubro principal según Google: "hamburger_restaurant", "bar", "butcher_shop". */
        String primaryType,
        /**
         * Todos los rubros, no solo el principal.
         *
         * Es lo que rescata a "La Birra Bar Colegiales", que figura como restaurante y
         * tiene hamburger_restaurant más abajo en la lista. Viene en el mismo tramo
         * barato que el resto y nunca lo usábamos.
         */
        Set<String> types
    ) {}
}
