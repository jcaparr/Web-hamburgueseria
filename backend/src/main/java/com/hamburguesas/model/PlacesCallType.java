package com.hamburguesas.model;

/** Google Places SKUs we consume, each with its own free monthly allowance. */
public enum PlacesCallType {
    SEARCH,
    /** Ficha de un local puntual, para los que ninguna búsqueda devuelve. */
    DETAILS,
    PHOTO,
    /**
     * El resumen de reseñas que arma Google, que es el tramo más caro de la API
     * (Enterprise + Atmosphere, mil gratis por mes). Se pide solo por los locales que
     * ninguna prueba barata resolvió.
     */
    RESUMEN,
    /**
     * El horario de apertura de un local. Es un campo Enterprise: mil gratis por mes, una
     * cuota aparte de la de fichas aunque la llamada sea la misma.
     */
    HORARIO
}
