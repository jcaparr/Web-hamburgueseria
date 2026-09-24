package com.hamburguesas.service;

import com.hamburguesas.dto.BurgerJointDto;
import com.hamburguesas.dto.TourDto;
import com.hamburguesas.dto.TourStopDto;
import com.hamburguesas.geo.Distancias;
import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.model.ModoDeViaje;
import com.hamburguesas.model.SavedTour;
import com.hamburguesas.repository.BurgerJointRepository;
import com.hamburguesas.repository.RatingRepository;
import com.hamburguesas.repository.SavedTourRepository;
import com.hamburguesas.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Arma un recorrido para salir a comer hamburguesas.
 *
 * Se le dice cuántas parar, cuántos kilómetros se está dispuesto a caminar y en qué
 * barrios; si no se nombra ninguno, se sale desde donde está quien camina. Devuelve las
 * paradas en el orden en que conviene hacerlas.
 *
 * El orden se arma yendo siempre a la más cercana de las que faltan. No es el recorrido
 * más corto que existe —encontrar ese es el problema del viajante, que no tiene solución
 * rápida— pero con cuatro o cinco paradas queda a un puñado de cuadras del mejor y se
 * calcula al instante.
 *
 * Pedirlo de nuevo tiene que proponer otro, y no cualquiera: ni el que está en pantalla
 * ni, si así se pidió, uno que esta persona ya haya guardado. Cuando el que sale es uno de
 * esos, se vuelve a armar sin su primera parada: obligado a arrancar en otro lado, pasa
 * por otro lado. Así el segundo recorrido sigue siendo el mejor posible de los que
 * quedan, en vez de uno sorteado.
 */
@Service
@RequiredArgsConstructor
public class TourService {

    /**
     * El tope de paradas.
     *
     * No es una decisión de diseño sino de Maps: un enlace de direcciones admite hasta
     * nueve escalas además del destino. Con más, el recorrido no se podría abrir.
     */
    public static final int MAXIMO_DE_PARADAS = 10;

    /**
     * Entre cuántas de las mejor puntuadas se sortea la primera parada.
     *
     * Empezar siempre por la mejor daría el mismo recorrido una y otra vez. Sorteando
     * entre las de arriba, volver a pedirlo propone otro sin que deje de empezar por una
     * que vale la pena.
     */
    private static final int CANDIDATAS_PARA_EMPEZAR = 12;

    /**
     * Cuántas veces se intenta antes de resignarse a repetir un recorrido.
     *
     * Cada intento descarta la primera parada del anterior, así que doce alcanzan para
     * recorrer bastante del barrio antes de rendirse. El tope existe porque con pocas
     * hamburgueserías se acaban las combinaciones: con tres locales y tres paradas hay un
     * solo recorrido posible, y guardado ese, no queda ninguno nuevo que ofrecer.
     */
    private static final int INTENTOS = 12;

    /** La nota que se le supone a una hamburguesería que todavía nadie puntuó. */
    private static final double SIN_RESENIAS = 3.0;

    private final BurgerJointRepository burgerJointRepository;
    private final RatingRepository ratingRepository;
    private final WishlistRepository wishlistRepository;
    private final SavedTourRepository savedTourRepository;

    public record Pedido(
        int cantidad,
        Double kilometrosMaximos,
        List<String> barrios,
        Double latitud,
        Double longitud,
        boolean incluirVisitadas,
        boolean conCadenas,
        ModoDeViaje modo,
        /** No proponer un recorrido que esta persona ya tenga guardado. */
        boolean evitarGuardados,
        /**
         * Los recorridos que ya se propusieron en esta vuelta, cada uno por los ids de sus
         * paradas.
         *
         * Es acumulativo y no solo el último a propósito: con uno solo, pedir otro
         * alternaba entre dos —el nuevo excluía al viejo, y el siguiente volvía al viejo
         * porque ya no estaba prohibido—. Mandándolos todos, cada vuelta propone uno que
         * no salió todavía.
         */
        List<List<Long>> distintoDe,
        /**
         * Fija el sorteo. La pantalla no la manda —quiere algo distinto cada vez— y los
         * tests sí, que es lo que los hace afirmables.
         */
        Long semilla
    ) {
        boolean sabeDondeEsta() {
            return latitud != null && longitud != null;
        }
    }

    public TourDto armar(Pedido pedido, Long userId) {
        int cantidad = Math.max(1, Math.min(MAXIMO_DE_PARADAS, pedido.cantidad()));

        Set<Long> visitadas = userId == null || pedido.incluirVisitadas()
            ? Set.of()
            : new HashSet<>(ratingRepository.idsPuntuadosPor(userId));

        List<BurgerJoint> candidatos = candidatos(pedido, visitadas);
        if (candidatos.isEmpty()) {
            return new TourDto(List.of(), 0, 0, 0,
                "No hay hamburgueserías que cumplan con lo que pediste");
        }

        Set<Set<Long>> prohibidos = combinacionesProhibidas(pedido, userId);

        // El primer intento es el mejor recorrido posible, sin nada descartado. Si resulta
        // ser uno de los prohibidos se vuelve a armar sin su primera parada: sacarla
        // obliga a arrancar en otro lado, y el recorrido que sale ya no puede ser el mismo
        // conjunto, porque le falta justamente esa.
        List<BurgerJoint> disponibles = candidatos;
        List<BurgerJoint> elMejor = null;
        List<BurgerJoint> paradas = List.of();
        boolean repetido = false;

        for (int intento = 0; intento < INTENTOS; intento++) {
            paradas = elegirParadas(pedido, disponibles, cantidad, semillaDe(pedido, intento));
            if (elMejor == null) {
                elMejor = paradas;
            }

            // Descartar paradas puede dejar el recorrido más corto que el primero. Uno más
            // corto no es una alternativa: quien pidió cuatro paradas quiere cuatro, y
            // recibir tres sin explicación se ve como una falla. Antes que eso se devuelve
            // el primero, repetido, que al menos está completo y se puede decir por qué.
            if (paradas.size() < elMejor.size()) {
                paradas = elMejor;
                repetido = true;
                break;
            }

            repetido = prohibidos.contains(idsDe(paradas));
            if (!repetido) {
                break;
            }

            Long primera = paradas.get(0).getId();
            disponibles = disponibles.stream()
                .filter(b -> !primera.equals(b.getId()))
                .toList();
            if (disponibles.isEmpty()) {
                paradas = elMejor;
                break;
            }
        }

        return comoTour(pedido, paradas, candidatos.size(), cantidad, userId, repetido);
    }

    /**
     * La semilla de cada intento.
     *
     * Sin semilla en el pedido se sortea de nuevo en cada llamada, que es lo que hace que
     * la pantalla proponga algo distinto cada vez. Con semilla el recorrido es siempre el
     * mismo, y los intentos siguen siendo distintos entre sí.
     */
    private static long semillaDe(Pedido pedido, int intento) {
        long base = pedido.semilla() != null ? pedido.semilla() : new Random().nextLong();
        return base + intento;
    }

    /** Los recorridos que esta vez no se pueden proponer. */
    private Set<Set<Long>> combinacionesProhibidas(Pedido pedido, Long userId) {
        Set<Set<Long>> prohibidos = new HashSet<>();

        if (pedido.distintoDe() != null) {
            pedido.distintoDe().stream()
                .filter(combinacion -> combinacion != null && !combinacion.isEmpty())
                .forEach(combinacion -> prohibidos.add(new HashSet<>(combinacion)));
        }

        if (pedido.evitarGuardados() && userId != null) {
            for (SavedTour guardado : savedTourRepository.findByUser_IdOrderByCreatedAtDesc(userId)) {
                prohibidos.add(guardado.idsDeLasParadas());
            }
        }

        return prohibidos;
    }

    /**
     * Por dónde pasa un recorrido, sin orden.
     *
     * Dos recorridos son el mismo cuando pasan por las mismas hamburgueserías: las mismas
     * cuatro caminadas en otro orden son la misma salida, no una nueva.
     */
    private static Set<Long> idsDe(List<BurgerJoint> paradas) {
        return paradas.stream().map(BurgerJoint::getId).collect(Collectors.toSet());
    }

    /** Lo que se puede visitar: con coordenadas, del barrio pedido y todavía sin probar. */
    private List<BurgerJoint> candidatos(Pedido pedido, Set<Long> visitadas) {
        List<BurgerJoint> todos = pedido.conCadenas()
            ? burgerJointRepository.findAll()
            : burgerJointRepository.findByFastFoodFalse();

        Set<String> barrios = pedido.barrios() == null ? Set.of() : new HashSet<>(pedido.barrios());

        return todos.stream()
            .filter(b -> b.getLatitude() != null && b.getLongitude() != null)
            .filter(b -> barrios.isEmpty() || barrios.contains(b.getArea()))
            .filter(b -> !visitadas.contains(b.getId()))
            .toList();
    }

    /** El promedio de cada local que tiene alguna reseña, de una sola consulta. */
    private Map<Long, Double> promedios() {
        Map<Long, Double> promedios = new HashMap<>();
        for (Object[] fila : ratingRepository.promediosPorLocal()) {
            promedios.put((Long) fila[0], ((Number) fila[1]).doubleValue());
        }
        return promedios;
    }

    /**
     * Las paradas, en el orden en que conviene hacerlas.
     *
     * La primera se sortea —entre las más cercanas si se sabe dónde está quien lo pide,
     * entre las mejor puntuadas si no—, y de ahí en adelante siempre se va a la más
     * cercana de las que quedan. Se corta al llegar a la cantidad pedida o cuando la
     * próxima no entra en los kilómetros que se está dispuesto a hacer.
     */
    private List<BurgerJoint> elegirParadas(
        Pedido pedido, List<BurgerJoint> candidatos, int cantidad, long semilla
    ) {
        List<BurgerJoint> quedan = new ArrayList<>(candidatos);
        List<BurgerJoint> paradas = new ArrayList<>();

        double kilometros = 0;
        double latitud = pedido.sabeDondeEsta() ? pedido.latitud() : 0;
        double longitud = pedido.sabeDondeEsta() ? pedido.longitud() : 0;

        while (paradas.size() < cantidad && !quedan.isEmpty()) {
            boolean esLaPrimeraSinPunto = paradas.isEmpty() && !pedido.sabeDondeEsta();

            BurgerJoint proxima = esLaPrimeraSinPunto
                ? primeraPorNota(quedan, semilla)
                : masCercana(quedan, latitud, longitud);

            double tramo = esLaPrimeraSinPunto
                ? 0
                : kilometrosEntre(pedido.modo(), latitud, longitud,
                    proxima.getLatitude(), proxima.getLongitude());

            // El tope no puede dejar el recorrido vacío: si la primera ya queda lejos, se
            // la acepta igual y lo que se ve es un recorrido de una parada, no uno de
            // ninguna. Llegar hasta la primera es ir, no recorrer.
            boolean seVaDeLargo = pedido.kilometrosMaximos() != null
                && !paradas.isEmpty()
                && kilometros + tramo > pedido.kilometrosMaximos();
            if (seVaDeLargo) {
                break;
            }

            paradas.add(proxima);
            quedan.remove(proxima);
            kilometros += tramo;
            latitud = proxima.getLatitude();
            longitud = proxima.getLongitude();
        }

        return paradas;
    }

    private BurgerJoint primeraPorNota(List<BurgerJoint> candidatos, long semilla) {
        Map<Long, Double> promedios = promedios();

        List<BurgerJoint> mejores = new ArrayList<>(candidatos.stream()
            // Sin reseñas no se sabe si es buena: va después de las bien puntuadas, pero
            // antes que las malas. No hay motivo para castigar a la que nadie probó.
            .sorted(Comparator.comparingDouble(
                (BurgerJoint b) -> promedios.getOrDefault(b.getId(), SIN_RESENIAS)).reversed())
            .limit(CANDIDATAS_PARA_EMPEZAR)
            .toList());

        Collections.shuffle(mejores, new Random(semilla));
        return mejores.get(0);
    }

    private BurgerJoint masCercana(List<BurgerJoint> candidatos, double latitud, double longitud) {
        return candidatos.stream()
            .min(Comparator.comparingDouble(b ->
                Distancias.metrosEntre(latitud, longitud, b.getLatitude(), b.getLongitude())))
            .orElseThrow();
    }

    /** Lo que se recorre de verdad entre dos paradas, que depende de cómo se vaya. */
    private static double kilometrosEntre(
        ModoDeViaje modo, double latA, double lonA, double latB, double lonB
    ) {
        return modo.kilometrosReales(Distancias.metrosEntre(latA, lonA, latB, lonB) / 1000);
    }

    private TourDto comoTour(
        Pedido pedido, List<BurgerJoint> paradas, int candidatos, int cantidad, Long userId,
        boolean repetido
    ) {
        Set<Long> puntuadas = userId == null
            ? Set.of()
            : new HashSet<>(ratingRepository.idsPuntuadosPor(userId));

        double latitud = pedido.sabeDondeEsta() ? pedido.latitud() : 0;
        double longitud = pedido.sabeDondeEsta() ? pedido.longitud() : 0;

        List<TourStopDto> stops = new ArrayList<>();
        double total = 0;

        for (int i = 0; i < paradas.size(); i++) {
            BurgerJoint parada = paradas.get(i);
            double tramo = i == 0 && !pedido.sabeDondeEsta()
                ? 0
                : kilometrosEntre(pedido.modo(), latitud, longitud,
                    parada.getLatitude(), parada.getLongitude());
            total += tramo;

            stops.add(new TourStopDto(i + 1, redondear(tramo),
                puntuadas.contains(parada.getId()), comoDto(parada, userId)));

            latitud = parada.getLatitude();
            longitud = parada.getLongitude();
        }

        return new TourDto(stops, redondear(total), pedido.modo().minutos(total), candidatos,
            aviso(paradas.size(), cantidad, candidatos, pedido, repetido));
    }

    /**
     * Qué no se pudo cumplir del pedido.
     *
     * Sin esto, pedir seis paradas y recibir tres —o recibir el mismo recorrido de recién—
     * se ve como un error de la app en vez de como la respuesta que es.
     */
    private static String aviso(
        int paradas, int cantidad, int candidatos, Pedido pedido, boolean repetido
    ) {
        if (repetido) {
            return "Ya recorriste todo lo que entra con estos filtros: este se repite";
        }
        if (paradas >= cantidad) {
            return null;
        }
        if (candidatos <= paradas) {
            return candidatos == 1
                ? "Con esos filtros hay una sola hamburguesería"
                : "Con esos filtros hay " + candidatos + " hamburgueserías";
        }
        return pedido.modo().comoSeDice() + " " + enKilometros(pedido.kilometrosMaximos())
            + " km entran " + (paradas == 1 ? "una parada" : paradas + " paradas");
    }

    private static String enKilometros(Double kilometros) {
        if (kilometros == null) {
            return "0";
        }
        return kilometros == Math.floor(kilometros)
            ? String.valueOf((long) (double) kilometros)
            : String.valueOf(kilometros);
    }

    private BurgerJointDto comoDto(BurgerJoint b, Long userId) {
        Double promedio = ratingRepository.averageScoreByBurgerJoint(b.getId());
        long resenias = ratingRepository.countByBurgerJoint_Id(b.getId());
        boolean enDeseos = userId != null
            && wishlistRepository.existsByUser_IdAndBurgerJoint_Id(userId, b.getId());

        return new BurgerJointDto(
            b.getId(), b.getPlaceId(), b.getName(), b.getAddress(), b.getArea(), b.getPhotoUrl(),
            b.getLatitude(), b.getLongitude(), promedio, resenias, enDeseos);
    }

    private static double redondear(double kilometros) {
        return Math.round(kilometros * 10) / 10.0;
    }
}
