package com.hamburguesas.places;

/**
 * Qué contestó Google sobre los locales que todavía no tienen foto, sin bajar ninguna.
 *
 * Existe porque son dos cuotas distintas y muy desparejas: preguntar si un local tiene
 * fotos sale una ficha —cuatro mil gratis por mes— y bajar una sale una foto, de las que
 * hay mil. Con mil doscientos locales sin portada, bajar mientras se pregunta gasta la
 * cuota chica entera en el orden en que aparecen en la base, y encima se la gasta en
 * locales que la limpieza va a borrar porque Google no tiene ni una foto de ellos.
 *
 * Preguntar primero cuesta lo barato y dice exactamente cuántos quedan en pie.
 */
public record CensoDeFichas(
    /** A cuántos se les alcanzó a preguntar antes de que se acabara la cuota de fichas. */
    int preguntados,
    /** De esos, cuántos Google dice no tener ninguna foto. Quedan anotados para la limpieza. */
    int sinNingunaFoto,
    /** Y cuántos sí tienen, que son los que merecen que se les gaste una foto. */
    int conFotosParaBajar,
    /** Cuántos quedaron sin preguntar, si la cuota de fichas no alcanzó. */
    int sinPreguntar,
    String warning
) {
    public static CensoDeFichas skipped(String warning) {
        return new CensoDeFichas(0, 0, 0, 0, warning);
    }
}
