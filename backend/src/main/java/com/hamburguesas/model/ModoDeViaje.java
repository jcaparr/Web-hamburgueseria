package com.hamburguesas.model;

/**
 * Cómo se hace el recorrido, que cambia cuánto se tarda y cuánto se da vuelta.
 *
 * Los dos números de cada modo salen de la misma idea: entre dos puntos no se puede ir
 * en diagonal, hay que hacer las cuadras. El factor es cuánto más se recorre que la
 * línea recta, y en auto es mucho más alto que a pie porque casi todas las calles de la
 * Ciudad son de una sola mano: donde el que camina cruza y sigue, el auto da la vuelta a
 * la manzana.
 *
 * Los cuatro números están contrastados contra Maps, con los enlaces que genera la
 * pantalla:
 *
 * <ul>
 *   <li>A pie, cuatro paradas en Palermo: 2,5 km y 34 minutos contra 2,6 y 36.
 *   <li>En auto, cinco paradas desde el centro hasta La Boca: 8,6 km y 32 minutos
 *       contra 8,6 y 33.
 * </ul>
 *
 * El factor del auto sigue quedando corto en los saltos de pocas cuadras, donde dar la
 * vuelta a la manzana pesa mucho más en proporción: un recorrido de cuatro paradas
 * pegadas en Palermo nos da 0,9 km contra 1,4 de Maps. Un solo número no puede valer
 * para las dos escalas, y se eligió el que acierta en los recorridos largos, que son los
 * que uno hace en auto.
 *
 * Son estimaciones para poder ordenar y filtrar el recorrido; el recorrido de verdad lo
 * da Maps cuando se abre.
 */
public enum ModoDeViaje {

    A_PIE(4.5, 1.3, "Caminando"),
    EN_AUTO(16, 1.8, "En auto");

    private final double kilometrosPorHora;
    private final double factorDeCalle;
    private final String comoSeDice;

    ModoDeViaje(double kilometrosPorHora, double factorDeCalle, String comoSeDice) {
        this.kilometrosPorHora = kilometrosPorHora;
        this.factorDeCalle = factorDeCalle;
        this.comoSeDice = comoSeDice;
    }

    /** Los kilómetros que se recorren de verdad entre dos puntos separados por esa recta. */
    public double kilometrosReales(double enLineaRecta) {
        return enLineaRecta * factorDeCalle;
    }

    public int minutos(double kilometros) {
        return (int) Math.round(kilometros / kilometrosPorHora * 60);
    }

    /** Para empezar una frase: "Caminando 3 km entran 2 paradas". */
    public String comoSeDice() {
        return comoSeDice;
    }
}
