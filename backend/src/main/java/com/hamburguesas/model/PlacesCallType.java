package com.hamburguesas.model;

/** Google Places SKUs we consume, each with its own free monthly allowance. */
public enum PlacesCallType {
    SEARCH,
    /** Ficha de un local puntual, para los que ninguna búsqueda devuelve. */
    DETAILS,
    PHOTO
}
