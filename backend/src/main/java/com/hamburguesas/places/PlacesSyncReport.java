package com.hamburguesas.places;

public record PlacesSyncReport(
    int created,
    int updated,
    int photosDownloaded,
    String warning
) {
    public static PlacesSyncReport skipped(String warning) {
        return new PlacesSyncReport(0, 0, 0, warning);
    }
}
