package com.hamburguesas.model;

/** Google Places SKUs we consume, each with its own free monthly allowance. */
public enum PlacesCallType {
    SEARCH,
    /**
     * Qué fotos tiene un local: su ficha, con solo el identificador y las fotos.
     *
     * Google no la cobra (#199). Reemplaza a DETAILS, que era la misma ficha con el nombre
     * agregado y por eso se cobraba como Pro. Se sigue contando, con un tope, por si
     * alguna vez se le agrega un campo pago sin darse cuenta: en ese caso el gasto queda
     * igual dentro de un tramo gratuito.
     *
     * Los meses anteriores quedan en la base como DETAILS, y la restricción de la tabla
     * los sigue aceptando.
     */
    LISTA_DE_FOTOS,
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
