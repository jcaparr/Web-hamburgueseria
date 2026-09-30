package com.hamburguesas.places;

import tools.jackson.databind.JsonNode;

/**
 * La foto que se eligió para un local: cómo pedirla y cómo reconocerla después.
 *
 * Las dos cosas hacen falta porque el nombre no sirve para reconocerla. Google devuelve
 * el nombre como "places/ChIJ.../photos/Aa-ngM..." y esa segunda parte cambia en cada
 * pedido: pidiendo la misma ficha dos veces con tres segundos de diferencia, ninguno de
 * los diez nombres coincidió. Es un vale para descargar, no un identificador.
 *
 * Por eso se guarda además una huella. Lo que sí se repite igual, y en el mismo orden,
 * es el tamaño y quién la subió; con eso alcanza para decidir si la foto que elegimos
 * hoy es la misma que ya tenemos bajada.
 *
 * Sin esto la comparación nunca daba verdadero y cada revisión se bajaba las
 * cuatrocientas fotos de nuevo, que es el tramo gratuito de un mes entero.
 */
public record FotoElegida(String name, String huella) {

    static FotoElegida de(JsonNode photo) {
        if (photo == null) {
            return null;
        }

        String nombre = photo.path("name").asText(null);
        if (nombre == null) {
            return null;
        }

        return new FotoElegida(nombre, huellaDe(photo));
    }

    /**
     * Ancho, alto y quién la subió.
     *
     * No pretende ser única en todo Google: solo distinguir entre las diez fotos de un
     * mismo local, que es para lo único que se compara. Dos fotos del mismo autor con
     * exactamente el mismo tamaño se confundirían, y la consecuencia sería no bajar una
     * foto que cambió: barato al lado de bajarlas todas siempre.
     */
    private static String huellaDe(JsonNode photo) {
        return photo.path("widthPx").asInt(0)
            + "x" + photo.path("heightPx").asInt(0)
            + "|" + photo.path("authorAttributions").path(0).path("displayName").asText("");
    }
}
