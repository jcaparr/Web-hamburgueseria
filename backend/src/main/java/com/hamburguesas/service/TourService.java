package com.hamburguesas.service;

import com.hamburguesas.dto.BurgerJointDto;
import com.hamburguesas.dto.TourDto;
import com.hamburguesas.dto.TourStopDto;
import com.hamburguesas.geo.Distancias;
import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.model.ModoDeViaje;
import com.hamburguesas.repository.BurgerJointRepository;
import com.hamburguesas.repository.RatingRepository;
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

    /** La nota que se le supone a una hamburguesería que todavía nadie puntuó. */
    private static final double SIN_RESENIAS = 3.0;

    private final BurgerJointRepository burgerJointRepository;
    private final RatingRepository ratingRepository;
    private final WishlistRepository wishlistRepository;

    public record Pedido(
        int cantidad,
        Double kilometrosMaximos,
        List<String> barrios,
        Double latitud,
        Double longitud,
        boolean incluirVisitadas,
        boolean conCadenas,
        ModoDeViaje modo,
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

        List<BurgerJoint> paradas = elegirParadas(pedido, candidatos, cantidad);
        return comoTour(pedido, paradas, candidatos.size(), cantidad, userId);
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
     * La primera se elige distinto según se sepa o no dónde está quien camina: si lo
     * dijo, es la que le queda más cerca; si no, se sortea entre las mejor puntuadas. De
     * ahí en adelante siempre se va a la más cercana de las que quedan, y se corta al
     * llegar a la cantidad pedida o cuando la próxima no entra en los kilómetros que se
     * está dispuesto a caminar.
     */
    private List<BurgerJoint> elegirParadas(
        Pedido pedido, List<BurgerJoint> candidatos, int cantidad
    ) {
        List<BurgerJoint> quedan = new ArrayList<>(candidatos);
        List<BurgerJoint> paradas = new ArrayList<>();

        double kilometros = 0;
        double latitud = pedido.sabeDondeEsta() ? pedido.latitud() : 0;
        double longitud = pedido.sabeDondeEsta() ? pedido.longitud() : 0;

        while (paradas.size() < cantidad && !quedan.isEmpty()) {
            boolean esLaPrimeraSinPunto = paradas.isEmpty() && !pedido.sabeDondeEsta();

            BurgerJoint proxima = esLaPrimeraSinPunto
                ? primeraPorNota(quedan, pedido.semilla())
                : masCercana(quedan, latitud, longitud);

            double tramo = esLaPrimeraSinPunto
                ? 0
                : kilometrosEntre(pedido.modo(), latitud, longitud,
                    proxima.getLatitude(), proxima.getLongitude());

            // El tope no puede dejar el recorrido vacío: si la primera ya queda lejos, se
            // la acepta igual y lo que se ve es un recorrido de una parada, no uno de
            // ninguna. Caminar hasta la primera es ir, no recorrer.
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

    private BurgerJoint primeraPorNota(List<BurgerJoint> candidatos, Long semilla) {
        Map<Long, Double> promedios = promedios();

        List<BurgerJoint> mejores = new ArrayList<>(candidatos.stream()
            // Sin reseñas no se sabe si es buena: va después de las bien puntuadas, pero
            // antes que las malas. No hay motivo para castigar a la que nadie probó.
            .sorted(Comparator.comparingDouble(
                (BurgerJoint b) -> promedios.getOrDefault(b.getId(), SIN_RESENIAS)).reversed())
            .limit(CANDIDATAS_PARA_EMPEZAR)
            .toList());

        Collections.shuffle(mejores, semilla == null ? new Random() : new Random(semilla));
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
        Pedido pedido, List<BurgerJoint> paradas, int candidatos, int cantidad, Long userId
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
            aviso(paradas.size(), cantidad, candidatos, pedido));
    }

    /**
     * Por qué salieron menos paradas de las pedidas.
     *
     * Sin esto, pedir seis y recibir tres se ve como un error de la app. Las dos razones
     * posibles son que no haya tantas hamburgueserías entre las que elegir o que no
     * entren en los kilómetros que se está dispuesto a caminar.
     */
    private static String aviso(int paradas, int cantidad, int candidatos, Pedido pedido) {
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
