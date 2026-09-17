package com.hamburguesas.places;

import java.util.List;

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
        String photoName
    ) {}
}
