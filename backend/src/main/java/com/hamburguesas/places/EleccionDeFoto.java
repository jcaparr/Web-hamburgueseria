package com.hamburguesas.places;

import com.hamburguesas.texto.Texto;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Qué foto de las que tiene un local en Google conviene usar de portada.
 *
 * Estaba adentro del cliente de Google, mezclada con los pedidos HTTP. Son dos cosas
 * distintas: el cliente sabe cómo preguntar, y esto decide qué hacer con la respuesta.
 * Separadas, la regla se puede leer entera de corrido y probar sin salir a la red.
 */
final class EleccionDeFoto {

    private EleccionDeFoto() {
    }

    /**
     * Qué versión de la regla de elección de foto es esta. Se guarda junto a cada foto
     * bajada, y cuando el número sube, las fotos elegidas con la regla anterior se
     * revisan una vez. Sin esto una mejora en la regla solo alcanzaría a los locales
     * nuevos, y los 424 que ya tienen foto se quedarían con la elección vieja.
     *
     * 1: la primera del local, o la primera de todas.
     * 2: la del local, prefiriendo apaisadas.
     * 3: además, descarta las capturas de pantalla.
     * 4: manda la forma sobre quién la subió, porque el local sube su marca.
     * 5: descarta el logo mirando los píxeles, y prueba la siguiente.
     */
    static final int REGLA_DE_FOTO = 5;

    /**
     * Cuántas fotos se prueban antes de darse por vencido.
     *
     * Tres: los locales con logo de portada suelen tener una o dos fotos de producto
     * atrás, y probar más sería gastar llamadas en un local que evidentemente no tiene
     * una foto buena.
     *
     * Lo aplica quien baja. La lista viene entera porque su largo es un dato que no
     * cuesta nada; el tope es sobre lo que se gasta, no sobre lo que se sabe.
     */
    static final int CANDIDATAS_A_PROBAR = 3;

    /**
     * Debajo de esta proporción la imagen es más alta que el doble de su ancho. Ninguna
     * cámara de celular saca así —las más estiradas dan 9:16, o sea 0,56—, con lo cual
     * lo que hay ahí es la pantalla entera de un teléfono capturada.
     */
    private static final double CAPTURA_DE_PANTALLA = 0.5;

    /**
     * Las mejores fotos del local, de la más prometedora a la menos.
     *
     * Las fotos llegan sin ninguna etiqueta de qué muestran: no hay forma de pedirle a
     * Google "el logo" o "la fachada". Lo que se busca es una hamburguesa que se vea
     * bien, y si no, el local bien fotografiado, y nada de eso viene dicho: se deduce de
     * lo único que Google cuenta de cada foto, que es quién la subió y qué tamaño tiene.
     *
     * Quién la subió pesaba más que todo lo demás, y resultó ser la señal equivocada.
     * El local sube su marca: de las diez fotos de "Keke & Larry", las dos suyas son el
     * logo y una promoción de empanadas, y la hamburguesa con papas la sacó un cliente.
     * Es lo razonable: al dueño le importa la identidad del local, y al que fue a comer
     * le importa el plato. Así que ahora la foto del local es un desempate y no una
     * garantía: una apaisada grande de un cliente le gana.
     *
     * Lo que ninguna de esas señales alcanza a distinguir es qué hay adentro de la foto,
     * y para eso está la elección a mano del otro método.
     *
     * Son varias y no una porque la elección no se termina de decidir acá: si la mejor
     * resulta ser un logo —cosa que solo se sabe mirando los píxeles, o sea después de
     * bajarla— hay que poder pasar a la siguiente en vez de dejar al local sin portada.
     */
    static List<FotoElegida> mejoresFotos(JsonNode place) {
        return mejoresFotos(place, null);
    }

    /**
     * Lo mismo, pero con una foto elegida a mano que va primera si está.
     *
     * Elegida a mano gana siempre, porque la regla puede ordenar por cómo está sacada la
     * foto y no por qué muestra: entre la fachada del local y una bandeja de empanadas no
     * hay nada en los datos que las distinga. Cuando alguien ya miró, lo que miró vale
     * más que cualquier puntaje.
     *
     * Las demás quedan atrás en el orden de siempre, que es lo que hace falta si la
     * elegida no se puede bajar.
     *
     * @param huellaElegida la huella anotada en la configuración, o null si no hay
     */
    static List<FotoElegida> mejoresFotos(JsonNode place, String huellaElegida) {
        JsonNode photos = place.path("photos");
        if (!photos.isArray() || photos.isEmpty()) {
            return List.of();
        }

        String placeName = place.path("displayName").path("text").asText("");

        List<JsonNode> ordenadas = new ArrayList<>();
        for (JsonNode photo : photos) {
            ordenadas.add(photo);
        }
        // Estable: ante igual puntaje queda primera la que Google muestra como principal.
        ordenadas.sort(Comparator.comparingInt((JsonNode photo) -> puntajeDe(photo, placeName))
            .reversed());

        // Todas y no solo las mejores tres. Cuántas tiene el local es un dato en sí
        // —diez es un lugar que la gente fotografía, tres es uno por el que nadie pasó—
        // y viene gratis en la misma respuesta. El tope de cuántas se prueban lo pone
        // quien las baja, que es donde se gasta la cuota.
        List<FotoElegida> candidatas = new ArrayList<>();
        for (JsonNode photo : ordenadas) {
            FotoElegida candidata = FotoElegida.de(photo);
            if (candidata != null) {
                candidatas.add(candidata);
            }
        }

        return conLaElegidaAdelante(candidatas, ordenadas, huellaElegida);
    }

    /**
     * Las primeras que vale la pena probar de la lista entera.
     *
     * La lista viene completa porque su largo dice cuántas fotos tiene el local, y eso
     * no cuesta nada. Pero cada una que se prueba cuesta una llamada de foto, así que el
     * tope se aplica recién acá: si las tres mejores resultaron logos, el local
     * evidentemente no tiene una portada buena y seguir es gastar.
     */
    static List<FotoElegida> aProbar(List<FotoElegida> candidatas) {
        return candidatas.size() <= CANDIDATAS_A_PROBAR
            ? candidatas
            : candidatas.subList(0, CANDIDATAS_A_PROBAR);
    }

    /**
     * Pone adelante la foto anotada a mano.
     *
     * Se la busca entre todas y no entre las candidatas: justamente se anota a mano
     * cuando la regla la dejó atrás, así que lo más probable es que no esté entre las
     * tres que la regla eligió. Si no aparece entre ninguna —la borraron, o la huella
     * está mal copiada— se devuelve la lista tal cual, y manda la regla.
     */
    private static List<FotoElegida> conLaElegidaAdelante(
        List<FotoElegida> candidatas, List<JsonNode> todas, String huellaElegida
    ) {
        if (huellaElegida == null || huellaElegida.isBlank()) {
            return candidatas;
        }

        for (JsonNode photo : todas) {
            FotoElegida foto = FotoElegida.de(photo);
            if (foto == null || !huellaElegida.equals(foto.huella())) {
                continue;
            }

            List<FotoElegida> conLaElegida = new ArrayList<>();
            conLaElegida.add(foto);
            candidatas.stream()
                .filter(otra -> !huellaElegida.equals(otra.huella()))
                .forEach(conLaElegida::add);
            return conLaElegida;
        }

        return candidatas;
    }

    /**
     * Qué tan buena es una foto como portada del local. Mayor es mejor, y ante empate
     * gana la primera, que es la que Google muestra como principal.
     *
     * Manda la forma de la foto por sobre quién la subió. Una apaisada grande de un
     * cliente le gana a una vertical del local, porque lo que el local sube suele ser
     * su marca y lo que sube el cliente suele ser lo que comió.
     */
    private static int puntajeDe(JsonNode photo, String placeName) {
        int puntaje = 0;

        // Alcanza para desempatar entre dos fotos de la misma forma, y no para que una
        // vertical del local le gane a una apaisada de un cliente.
        if (laSubioElLocal(photo, placeName)) {
            puntaje += 15;
        }

        // Las tarjetas recortan la imagen a 4:3, así que una foto vertical —el plato
        // que saca un cliente desde arriba— queda recortada al centro y se pierde el
        // local. Las fachadas y las portadas que sube el dueño suelen ser apaisadas.
        double proporcion = proporcionDe(photo);
        if (proporcion >= 1.2) {
            puntaje += 20;
        } else if (proporcion > 0 && proporcion < CAPTURA_DE_PANTALLA) {
            // Más alta que el doble de su ancho no es una foto sacada con la cámara:
            // es una captura de pantalla. "Valentino Burger" tenía de portada una
            // captura de una historia de Instagram —1080x2400, con el nombre de quien
            // la publicó arriba y el "Enviar mensaje" abajo—, y como la había subido el
            // propio local se quedaba con la portada. El castigo alcanza para que
            // pierda contra cualquier foto apaisada, incluso la de un cliente.
            puntaje -= 150;
        } else if (proporcion <= 0.85) {
            puntaje -= 20;
        }

        // Entre dos parecidas, la más grande: las chicas suelen ser logos recortados o
        // capturas, y encima se ven mal estiradas en la portada del detalle. Los dos
        // escalones son para que el tamaño desempate sin dar vuelta la forma.
        int ancho = photo.path("widthPx").asInt(0);
        if (ancho >= 2000) {
            puntaje += 6;
        } else if (ancho >= 1000) {
            puntaje += 3;
        }

        return puntaje;
    }

    private static double proporcionDe(JsonNode photo) {
        int alto = photo.path("heightPx").asInt(0);
        return alto == 0 ? 0 : (double) photo.path("widthPx").asInt(0) / alto;
    }

    /**
     * Si la foto la subió el propio local.
     *
     * No se pide igualdad exacta porque casi nunca la hay: el local figura en Google
     * como "Valentino" y en el mapa como "Valentino Burger", o al revés, "Mi Barrio
     * Hamburguesería" sube las fotos de "Mi Barrio Hamburguesería Caballito". Con
     * igualdad exacta se perdían 2 de cada 10 fotos oficiales.
     *
     * Alcanza con que un nombre contenga al otro, ignorando acentos, mayúsculas y
     * puntuación. Se exige un mínimo de 4 caracteres para que un local de nombre muy
     * corto no se quede con la foto de cualquier persona que se llame parecido.
     */
    private static boolean laSubioElLocal(JsonNode photo, String placeName) {
        String local = Texto.soloLetrasYNumeros(placeName);
        if (local.length() < 4) {
            return false;
        }

        for (JsonNode author : photo.path("authorAttributions")) {
            String autor = Texto.soloLetrasYNumeros(author.path("displayName").asText(""));
            if (autor.length() >= 4 && (autor.contains(local) || local.contains(autor))) {
                return true;
            }
        }
        return false;
    }
}
