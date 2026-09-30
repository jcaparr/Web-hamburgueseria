package com.hamburguesas.repository;

import com.hamburguesas.model.BurgerJoint;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

    static Specification<BurgerJoint> con(String nombre, String barrio, boolean conCadenas) {
        return (local, consulta, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();

            if (tieneAlgo(nombre)) {
                // El % y el _ que alguien escriba en el buscador son comodines del LIKE:
                // sin escaparlos, buscar "%" devuelve el listado entero.
                String patron = "%" + nombre.toLowerCase(Locale.ROOT)
                    .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
                condiciones.add(cb.like(cb.lower(local.get("name")), patron, '\\'));
            }

            if (tieneAlgo(barrio)) {
                condiciones.add(cb.equal(local.get("area"), barrio));
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
}
