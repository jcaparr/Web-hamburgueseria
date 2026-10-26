package com.hamburguesas.logros;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Avisa que cambió algo de lo que mide algún logro y hay que recalcular estas cuentas (#314).
 *
 * Se publica adentro de la transacción del cambio y se atiende cuando esa transacción ya
 * confirmó, así un recálculo que falle no se lleva puesto lo que la persona hizo.
 * Lo que se pasa por alto acá lo corrige la tarea de la noche, que recalcula a todos.
 */
public record LogrosPorRecalcular(Set<Long> cuentas) {

    public static LogrosPorRecalcular de(Long... cuentas) {
        Set<Long> unicas = new LinkedHashSet<>();
        for (Long cuenta : cuentas) {
            if (cuenta != null) {
                unicas.add(cuenta);
            }
        }
        return new LogrosPorRecalcular(unicas);
    }

    /** Una cuenta y, además, otras (quienes reaccionaron, quienes siguen...). */
    public static LogrosPorRecalcular de(Long cuenta, Collection<Long> otras) {
        Set<Long> unicas = new LinkedHashSet<>(otras);
        if (cuenta != null) {
            unicas.add(cuenta);
        }
        return new LogrosPorRecalcular(unicas);
    }
}
