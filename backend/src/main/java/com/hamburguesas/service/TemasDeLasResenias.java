package com.hamburguesas.service;

import com.hamburguesas.texto.Texto;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * De qué hablan las reseñas de un local, contado sobre lo que escribió la gente acá.
 *
 * El promedio dice qué tan buena es una hamburguesería y la distribución dice si hay
 * acuerdo, pero ninguno de los dos dice por qué. Eso está escrito en los comentarios, y
 * leer veinte para enterarse de que casi todos hablan bien de la carne y mal de la
 * espera es justamente lo que nadie hace.
 *
 * No interpreta el texto: busca de qué se habla y cuenta, y para saber si hablaron bien
 * o mal usa la nota que esa misma persona puso. Es una señal que ya está, que la puso
 * quien escribió y no nosotros, y que no se equivoca con la ironía ni con el "no estaba
 * nada mal". Lo que se pierde es el matiz —alguien puede poner 5 y quejarse del
 * precio—, y a cambio no inventa nada.
 */
public final class TemasDeLasResenias {

    private TemasDeLasResenias() {}

    /** Una reseña, reducida a lo que hace falta para contar: la nota y el texto. */
    public record Mencion(int nota, String comentario) {}

    /**
     * Para qué lado juega un tema: lo que gusta, lo que no, o lo que divide.
     *
     * Vive acá y no en la pantalla para que el corte esté en un solo lugar y probado.
     */
    public enum Tono { A_FAVOR, EN_CONTRA, DIVIDIDO }

    /** De cuántas reseñas habla un tema y en cuántas de esas la nota fue buena. */
    public record Tema(String nombre, int menciones, int aFavor) {

        /**
         * Tres estados y no un degradé, con cortes holgados —dos tercios para cada
         * lado— para que un tema no cambie de grupo por una sola reseña nueva.
         */
        public Tono tono() {
            if (aFavor * 3 >= menciones * 2) {
                return Tono.A_FAVOR;
            }
            if (aFavor * 3 <= menciones) {
                return Tono.EN_CONTRA;
            }
            return Tono.DIVIDIDO;
        }
    }

    /**
     * Cuántas reseñas con comentario hacen falta para mostrar algo.
     *
     * Con dos no hay un "de qué habla la gente": hay dos personas. Resumir eso sería
     * presentar una opinión suelta como si fuera una tendencia, que es peor que no
     * mostrar nada, porque lo de abajo ya está —las reseñas enteras, con nombre.
     */
    static final int RESENIAS_PARA_RESUMIR = 3;

    /** Un tema del que habló una sola persona tampoco es un tema. */
    private static final int MENCIONES_PARA_CONTAR = 2;

    /**
     * Cuántos se muestran: los de más abajo son ruido y la ficha no es un informe.
     *
     * Eran cuatro en una lista; repartidos en "lo que gusta" y "lo que no" entran seis
     * sin que se haga largo (#392).
     */
    private static final int TEMAS_A_MOSTRAR = 6;

    /** De 4 para arriba se cuenta como que habló bien. */
    private static final int NOTA_BUENA = 4;

    /**
     * De qué se puede hablar y con qué palabras se lo nombra.
     *
     * Son pocas y elegidas a mano a propósito. Una lista larga empieza a agarrar
     * palabras que significan otra cosa según la frase, y el error no se ve: queda un
     * tema contado de más que nadie puede auditar. El orden importa: ante igual
     * cantidad de menciones se muestra primero el que está más arriba.
     */
    private static final Map<String, List<String>> VOCABULARIO = new LinkedHashMap<>();

    static {
        // Las palabras van sin acentos ni eñes, como queda el texto después de normalizarlo.
        // Se dejan afuera las que cambian de sentido según la frase: "gusto" es también
        // "me gustó", "punto" es también "punto de encuentro", "mayo" es también el mes.
        VOCABULARIO.put("La carne", List.of("carne", "medallon", "medallones", "smash", "blend",
            "jugosa", "jugosas", "jugoso"));
        VOCABULARIO.put("Las papas", List.of("papas", "fritas"));
        VOCABULARIO.put("El pan", List.of("pan", "brioche", "bollo"));
        VOCABULARIO.put("El queso", List.of("queso", "cheddar"));
        VOCABULARIO.put("La panceta", List.of("panceta", "bacon"));
        VOCABULARIO.put("La salsa", List.of("salsa", "salsas", "alioli", "barbacoa"));
        VOCABULARIO.put("La cebolla", List.of("cebolla", "cebollas", "caramelizada"));
        VOCABULARIO.put("El sabor", List.of("sabor", "sabores", "sabrosa", "sabroso", "sabrosas"));
        VOCABULARIO.put("El tamaño", List.of("tamano", "porcion", "porciones", "abundante", "abundantes"));
        VOCABULARIO.put("Las bebidas", List.of("bebida", "bebidas", "cerveza", "cervezas", "birra",
            "gaseosa", "limonada"));
        VOCABULARIO.put("El precio", List.of("precio", "precios", "caro", "cara", "barato", "barata"));
        VOCABULARIO.put("La atención", List.of("atencion", "atendieron", "amables", "mozo", "moza"));
        VOCABULARIO.put("La espera", List.of("espera", "esperamos", "demora", "demoraron", "tardaron",
            "rapido", "rapidez"));
        VOCABULARIO.put("El lugar", List.of("lugar", "local", "ambiente", "mesas"));
        VOCABULARIO.put("El delivery", List.of("delivery", "envio", "rappi", "pedidosya"));
    }

    /**
     * Cada palabra del vocabulario, ya compilada, buscada como palabra entera.
     *
     * Entera y no como pedazo: "caro" adentro de "carozo" no habla del precio, y "pan"
     * adentro de "panceta" no habla del pan. Sin esto, la panceta —que en una
     * hamburguesería se nombra seguido— contaba como una mención al pan.
     */
    private static final Map<String, List<Pattern>> PATRONES = new LinkedHashMap<>();

    static {
        VOCABULARIO.forEach((tema, palabras) -> PATRONES.put(tema, palabras.stream()
            .map(palabra -> Pattern.compile("\\b" + Pattern.quote(palabra) + "\\b"))
            .toList()));
    }

    /**
     * Los temas de los que más se habló, del más nombrado al menos.
     *
     * Vacío si no hay reseñas escritas suficientes, que es la mayoría de los locales:
     * quien muestra esto tiene que estar preparado para no mostrar nada.
     */
    public static List<Tema> de(List<Mencion> resenias) {
        List<Mencion> escritas = resenias.stream()
            .filter(m -> m.comentario() != null && !m.comentario().isBlank())
            .toList();

        if (escritas.size() < RESENIAS_PARA_RESUMIR) {
            return List.of();
        }

        Map<String, int[]> cuenta = new LinkedHashMap<>();
        for (Mencion mencion : escritas) {
            // Sin acentos porque la mitad escribe "atencion", y en minúsculas porque la otra
            // mitad escribe gritando. Los signos quedan: buscar la palabra entera ya trata
            // "¡las papas!" igual que "las papas".
            String texto = Texto.sinAcentosEnMinuscula(mencion.comentario());
            for (String tema : PATRONES.keySet()) {
                // Una vez por reseña y no una por palabra: quien escribe "la carne, qué
                // carne" habló de la carne una sola vez, no dos.
                if (nombra(tema, texto)) {
                    int[] marcador = cuenta.computeIfAbsent(tema, t -> new int[2]);
                    marcador[0]++;
                    if (mencion.nota() >= NOTA_BUENA) {
                        marcador[1]++;
                    }
                }
            }
        }

        List<Tema> temas = new ArrayList<>();
        cuenta.forEach((tema, marcador) -> {
            if (marcador[0] >= MENCIONES_PARA_CONTAR) {
                temas.add(new Tema(tema, marcador[0], marcador[1]));
            }
        });

        // Estable: el orden del vocabulario desempata, así que dos temas con las mismas
        // menciones no se dan vuelta solos entre una visita y la siguiente.
        temas.sort(Comparator.comparingInt(Tema::menciones).reversed());
        return temas.size() > TEMAS_A_MOSTRAR ? temas.subList(0, TEMAS_A_MOSTRAR) : temas;
    }

    private static boolean nombra(String tema, String texto) {
        return PATRONES.get(tema).stream().anyMatch(patron -> patron.matcher(texto).find());
    }
}
