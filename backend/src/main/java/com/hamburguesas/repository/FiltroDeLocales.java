package com.hamburguesas.repository;

import com.hamburguesas.geo.Distancias;
import com.hamburguesas.geo.Ubicacion;
import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.model.Rating;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;


/**
 * Los filtros de Explorar, armados solo con los que llegaron.
 *
 * Antes eran cuatro consultas fijas —con nombre y sin él, con cadenas y sin ellas—
 * porque la forma natural, un "(:nombre is null or ...)", no funciona: con el parámetro
 * en nulo Postgres no puede deducir de qué tipo es, lo toma como binario y la consulta
 * falla entera con "no existe la función lower(bytea)". Los tests corren sobre H2, que
 * sí lo deduce, y no lo veían.
 *
 * Sumar el barrio llevaba esas cuatro a ocho. Armar la condición en vez de escribirla
 * resuelve las dos cosas: el filtro que no vino no aparece en el SQL, así que no hay
 * parámetro que Postgres tenga que adivinar, y agregar el próximo filtro es un if.
 */
final class FiltroDeLocales {

    private FiltroDeLocales() {
    }

    /** Hasta dónde llega cada franja de "más cercanas", en metros. Lo que pasa de la última es una sola. */
    private static final double[] FRANJAS_EN_METROS = {1_000, 2_000, 4_000, 8_000};

    static Specification<BurgerJoint> con(String nombre, List<String> barrios, OrdenDeLocales orden,
                                          Ubicacion ubicacion) {
        return con(nombre, barrios, orden, ubicacion, false);
    }

    static Specification<BurgerJoint> con(String nombre, List<String> barrios, OrdenDeLocales orden,
                                          Ubicacion ubicacion, boolean paraResenia) {
        return (local, consulta, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();

            if (tieneAlgo(nombre)) {
                // Se busca contra el nombre sin acentos, y lo que se escribió se le saca
                // los acentos con la misma regla: nadie los escribe en un buscador, y
                // "Atiko" tiene que encontrar a "Átiko".
                //
                // El % y el _ que alguien escriba son comodines del LIKE: sin escaparlos,
                // buscar "%" devuelve el listado entero.
                //
                // Para reseñar (#335) cada palabra tiene que aparecer en el nombre o en el
                // barrio: "mostaza belgrano" pide "mostaza" en uno de los dos y "belgrano"
                // en uno de los dos. Explorar no cambia: la frase entera, contra el nombre.
                if (paraResenia) {
                    for (String palabra : BurgerJoint.sinAcentos(nombre).split(" ")) {
                        if (palabra.isEmpty()) {
                            continue;
                        }
                        String patron = "%" + escapar(palabra) + "%";
                        condiciones.add(cb.or(
                            cb.like(local.get("nombreParaBuscar"), patron, '\\'),
                            cb.like(local.get("areaParaBuscar"), patron, '\\')));
                    }
                } else {
                    String patron = "%" + escapar(BurgerJoint.sinAcentos(nombre)) + "%";
                    condiciones.add(cb.like(local.get("nombreParaBuscar"), patron, '\\'));
                }
            }

            // Varios barrios se leen como "o": quien marca Palermo y Villa Crespo quiere
            // ver los dos, no los locales que estén en los dos a la vez, que no existen.
            //
            // Los vacíos se descartan antes de armar el IN: una lista de un solo elemento
            // vacío —que es lo que llega de un "?area=" suelto en la dirección— daría un
            // IN ('') y la pantalla saldría vacía sin que se vea por qué.
            List<String> conNombre = losQueDicenAlgo(barrios);
            if (!conNombre.isEmpty()) {
                condiciones.add(local.get("area").in(conNombre));
            }

            // Las cadenas no salen nunca en Explorar, ni buscándolas por nombre: eso lo
            // contesta /api/cadenas, con una tarjeta por cadena y no una por sucursal
            // (#206). Quién es cadena lo decide FastFoodMarker al arrancar, así que acá
            // alcanza con mirar la columna.
            //
            // La pantalla de reseña sí las incluye (paraResenia): se reseña una sucursal.
            if (!paraResenia) {
                condiciones.add(cb.isFalse(local.get("fastFood")));
            }

            ordenar(local, consulta, cb, orden, ubicacion);

            // Sin nombre ni barrio queda solo la de las cadenas: todas las hamburgueserías.
            return cb.and(condiciones.toArray(new Predicate[0]));
        };
    }

    /**
     * El orden de Explorar (#207), según la opción elegida: los de más reseñas, los
     * mejores valorados o los peores. Ver {@link OrdenDeLocales}.
     *
     * No tenía ninguno, y Postgres los devolvía en el orden en que estaban guardados en
     * el disco, que cambia cada vez que se actualiza un local. La lista se reacomodaba
     * sola, y entre una página y la siguiente podía repetir o saltear locales.
     *
     * En las tres:
     * <ul>
     *   <li>Primero, si tiene reseñas. Los que no tienen van al final siempre, también en
     *   "peores valoradas", que si no arrancaría con los mil que nadie probó.</li>
     *   <li>Al final, el nombre y el id. El nombre es el que se usa para buscar, sin
     *   acentos ni mayúsculas: "Átiko" va con las de la A. El id hace que el orden sea
     *   uno solo: dos locales sin reseñas y con el mismo nombre, que los hay, salen
     *   siempre en el mismo lugar.</li>
     * </ul>
     *
     * No se ordena la consulta que cuenta cuántos hay. Spring le saca el orden igual,
     * pero armar las subconsultas para nada es gasto.
     */
    private static void ordenar(Root<BurgerJoint> local, CriteriaQuery<?> consulta, CriteriaBuilder cb,
                                OrdenDeLocales orden, Ubicacion ubicacion) {
        Class<?> tipo = consulta.getResultType();
        if (tipo == Long.class || tipo == long.class) {
            return;
        }

        // Sin desde dónde medir no hay "cercanas": se ordena por relevancia, como siempre.
        if (orden == OrdenDeLocales.CERCANAS && ubicacion == null) {
            orden = OrdenDeLocales.RELEVANTES;
        }

        Subquery<Double> promedio = consulta.subquery(Double.class);
        Root<Rating> resenia = promedio.from(Rating.class);
        promedio.select(cb.avg(resenia.get("score")))
            .where(cb.equal(resenia.get("burgerJoint"), local));

        Subquery<Long> cuantas = consulta.subquery(Long.class);
        Root<Rating> otra = cuantas.from(Rating.class);
        cuantas.select(cb.count(otra))
            .where(cb.equal(otra.get("burgerJoint"), local));

        Expression<Integer> sinResenias = cb.<Integer>selectCase()
            .when(cb.equal(cuantas, 0L), 1)
            .otherwise(0);

        List<Order> criterios = new ArrayList<>();
        if (orden == OrdenDeLocales.CERCANAS) {
            criterios.add(cb.asc(franjaDeDistancia(local, cb, ubicacion)));
        }
        criterios.add(cb.asc(sinResenias));
        switch (orden) {
            // Dentro de cada franja, la calificación (#288): igual que "mejores valoradas".
            case CERCANAS -> {
                criterios.add(cb.desc(promedio));
                criterios.add(cb.desc(cuantas));
            }
            case RELEVANTES -> {
                criterios.add(cb.desc(cuantas));
                criterios.add(cb.desc(promedio));
            }
            case MEJORES -> {
                criterios.add(cb.desc(promedio));
                criterios.add(cb.desc(cuantas));
            }
            case PEORES -> {
                criterios.add(cb.asc(promedio));
                criterios.add(cb.desc(cuantas));
            }
        }
        criterios.add(cb.asc(local.get("nombreParaBuscar")));
        criterios.add(cb.asc(local.get("id")));
        consulta.orderBy(criterios);
    }

    /**
     * En qué franja de distancia cae el local: 0 es la más cercana. Los que no tienen
     * coordenadas van en una franja propia, al final.
     *
     * La cuenta es la de {@code Distancias.metrosEntre} (la Tierra tratada como un plano,
     * que a estas distancias se equivoca en centímetros) pero comparando contra el
     * cuadrado de cada franja, así no hace falta la raíz en SQL. Corre en la base: son
     * cuatrocientos y pico locales, y no hay llamada a ningún servicio externo.
     */
    private static Expression<Integer> franjaDeDistancia(Root<BurgerJoint> local, CriteriaBuilder cb,
                                                          Ubicacion ubicacion) {
        Expression<Double> dx = cb.prod(
            cb.diff(local.<Double>get("longitude"), ubicacion.longitud()),
            Distancias.METROS_POR_GRADO_DE_LONGITUD);
        Expression<Double> dy = cb.prod(
            cb.diff(local.<Double>get("latitude"), ubicacion.latitud()),
            Distancias.METROS_POR_GRADO_DE_LATITUD);
        Expression<Double> cuadrado = cb.sum(cb.prod(dx, dx), cb.prod(dy, dy));

        CriteriaBuilder.Case<Integer> franja = cb.<Integer>selectCase();
        franja.when(cb.or(cb.isNull(local.get("latitude")), cb.isNull(local.get("longitude"))),
            FRANJAS_EN_METROS.length + 1);
        for (int i = 0; i < FRANJAS_EN_METROS.length; i++) {
            franja.when(cb.lessThan(cuadrado, FRANJAS_EN_METROS[i] * FRANJAS_EN_METROS[i]), i);
        }
        return franja.otherwise(FRANJAS_EN_METROS.length);
    }

    private static String escapar(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static boolean tieneAlgo(String valor) {
        return valor != null && !valor.isBlank();
    }

    /**
     * Los barrios que de verdad dicen algo, para no armar un IN con basura adentro.
     *
     * Una dirección puede traer "?area=" suelto —el navegador la deja así al borrar la
     * selección, y también sale de armarla a mano—. Eso llega como una lista con un
     * elemento vacío, que no es lo mismo que una lista vacía: daría un IN ('') y la
     * pantalla saldría sin resultados sin que se vea por qué.
     *
     * Devuelve lista vacía cuando no quedó ninguno, que es lo que quien llama lee como
     * "no filtrar por barrio".
     */
    static List<String> losQueDicenAlgo(List<String> barrios) {
        return barrios == null ? List.of()
            : barrios.stream().filter(FiltroDeLocales::tieneAlgo).toList();
    }
}
