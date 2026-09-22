package com.hamburguesas.places;

public record PlacesSyncReport(
    int created,
    int updated,
    int photosDownloaded,
    /** Las que salieron de otra sucursal de la misma cadena, sin llamar a Google. */
    int photosReused,
    String warning
) {
    public static PlacesSyncReport skipped(String warning) {
        return new PlacesSyncReport(0, 0, 0, 0, warning);
    }
}
