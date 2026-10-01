package com.hamburguesas.repository;

import com.hamburguesas.model.BurgerJoint;
import jakarta.persistence.criteria.Predicate;
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

    static Specification<BurgerJoint> con(String nombre, List<String> barrios, boolean conCadenas) {
        return (local, consulta, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();

            if (tieneAlgo(nombre)) {
                // Se busca contra el nombre sin acentos, y lo que se escribió se le saca
                // los acentos con la misma regla: nadie los escribe en un buscador, y
                // "Atiko" tiene que encontrar a "Átiko".
                //
                // El % y el _ que alguien escriba son comodines del LIKE: sin escaparlos,
                // buscar "%" devuelve el listado entero.
                String patron = "%" + BurgerJoint.sinAcentos(nombre)
                    .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
                condiciones.add(cb.like(local.get("nombreParaBuscar"), patron, '\\'));
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

            // Quién es cadena lo decide FastFoodMarker al arrancar, así que acá alcanza
            // con mirar la columna.
            if (!conCadenas) {
                condiciones.add(cb.isFalse(local.get("fastFood")));
            }

            // Sin ninguna condición esto da "1 = 1", que es el listado completo.
            return cb.and(condiciones.toArray(new Predicate[0]));
        };
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
