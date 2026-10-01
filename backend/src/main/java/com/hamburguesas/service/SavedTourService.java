package com.hamburguesas.service;

import com.hamburguesas.dto.GuardarTourRequest;
import com.hamburguesas.dto.SavedTourDto;
import com.hamburguesas.dto.TourStopDto;
import com.hamburguesas.exception.ConflictException;
import com.hamburguesas.exception.ResourceNotFoundException;
import com.hamburguesas.geo.Distancias;
import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.model.ModoDeViaje;
import com.hamburguesas.model.SavedTour;
import com.hamburguesas.model.SavedTourStop;
import com.hamburguesas.model.User;
import com.hamburguesas.repository.BurgerJointRepository;
import com.hamburguesas.repository.RatingRepository;
import com.hamburguesas.repository.SavedTourRepository;
import com.hamburguesas.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Los recorridos que alguien guardó.
 *
 * Se guardan las paradas y el modo, y nada más: los kilómetros y los minutos los
 * recalcula acá el servidor. La pantalla los tiene, pero son números que después se
 * muestran como propios, y no hay razón para creerle a lo que mande el navegador.
 *
 * Los kilómetros del guardado cuentan solo lo que hay entre paradas, aunque el recorrido
 * se haya armado saliendo desde donde estaba esa persona: ese primer tramo era de ese día
 * y de ese lugar, y la próxima vez se sale desde otro lado.
 */
@Service
@RequiredArgsConstructor
public class SavedTourService {

    /** Más que esto y la lista del perfil deja de ser una lista. */
    private static final int MAXIMO_POR_PERSONA = 50;

    private final SavedTourRepository savedTourRepository;
    private final BurgerJointRepository burgerJointRepository;
    private final RatingRepository ratingRepository;
    private final UserRepository userRepository;
    private final FichaDeLocal fichaDeLocal;

    @Transactional
    public SavedTourDto guardar(Long userId, GuardarTourRequest request) {
        List<Long> ids = sinRepetidos(request.paradas());

        if (savedTourRepository.countByUser_Id(userId) >= MAXIMO_POR_PERSONA) {
            throw new ConflictException(
                "Llegaste a " + MAXIMO_POR_PERSONA + " recorridos guardados. Borrá alguno.");
        }

        List<BurgerJoint> paradas = enElOrdenPedido(ids);
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("No encontramos tu cuenta"));

        SavedTour tour = SavedTour.builder()
            .user(user)
            .name(nombreDe(paradas))
            .travelMode(request.modo())
            .build();

        double total = 0;
        for (int i = 0; i < paradas.size(); i++) {
            BurgerJoint parada = paradas.get(i);
            double tramo = i == 0 ? 0 : kilometrosEntre(request.modo(), paradas.get(i - 1), parada);
            total += tramo;

            tour.getStops().add(SavedTourStop.builder()
                .tour(tour).burgerJoint(parada).position(i + 1).kilometers(Distancias.aUnDecimal(tramo))
                .build());
        }

        tour.setKilometers(Distancias.aUnDecimal(total));
        tour.setMinutes(request.modo().minutos(total));

        return comoDto(savedTourRepository.save(tour));
    }

    @Transactional(readOnly = true)
    public List<SavedTourDto> mios(Long userId) {
        return savedTourRepository.findByUser_IdOrderByCreatedAtDesc(userId).stream()
            .map(this::comoDto)
            .toList();
    }

    /** Por id y dueño a la vez: nadie borra el recorrido de otro. */
    @Transactional
    public void borrar(Long userId, Long tourId) {
        SavedTour tour = savedTourRepository.findByIdAndUser_Id(tourId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Ese recorrido no existe"));
        savedTourRepository.delete(tour);
    }

    /**
     * Las paradas en el orden en que las mandaron.
     *
     * findAllById no respeta el orden del pedido y puede devolver de menos si alguna ya no
     * está: un local que la limpieza borró entre que se armó el recorrido y se lo guardó.
     */
    private List<BurgerJoint> enElOrdenPedido(List<Long> ids) {
        Map<Long, BurgerJoint> porId = burgerJointRepository.findAllById(ids).stream()
            .collect(Collectors.toMap(BurgerJoint::getId, Function.identity()));

        List<BurgerJoint> paradas = new ArrayList<>();
        for (Long id : ids) {
            BurgerJoint parada = porId.get(id);
            if (parada == null) {
                throw new ResourceNotFoundException(
                    "Una de las hamburgueserías del recorrido ya no está");
            }
            paradas.add(parada);
        }
        return paradas;
    }

    /** Conserva el orden y saca los repetidos, que harían un recorrido que vuelve sobre sí. */
    private static List<Long> sinRepetidos(List<Long> ids) {
        List<Long> unicos = new ArrayList<>(new LinkedHashSet<>(ids));
        if (unicos.size() < 2) {
            throw new ConflictException("Un recorrido necesita al menos dos paradas distintas");
        }
        return unicos;
    }

    /**
     * El nombre sale de los barrios y la cantidad: nadie quiere ponerle nombre a esto.
     *
     * Con más de dos barrios se dice cuántos, porque la lista entera no entra en una
     * tarjeta y tampoco agrega nada.
     */
    static String nombreDe(List<BurgerJoint> paradas) {
        Set<String> barrios = paradas.stream()
            .map(BurgerJoint::getArea)
            .filter(area -> area != null && !area.isBlank())
            .collect(Collectors.toCollection(LinkedHashSet::new));

        String donde = switch (barrios.size()) {
            case 0 -> "la Ciudad";
            case 1, 2 -> String.join(" y ", barrios);
            default -> barrios.size() + " barrios";
        };

        return paradas.size() + " paradas por " + donde;
    }

    private static double kilometrosEntre(ModoDeViaje modo, BurgerJoint desde, BurgerJoint hasta) {
        return modo.kilometrosReales(Distancias.metrosEntre(
            desde.getLatitude(), desde.getLongitude(),
            hasta.getLatitude(), hasta.getLongitude()) / 1000);
    }

    private SavedTourDto comoDto(SavedTour tour) {
        Set<Long> puntuadas = new HashSet<>(
            ratingRepository.idsPuntuadosPor(tour.getUser().getId()));

        List<TourStopDto> paradas = tour.getStops().stream()
            .map(stop -> new TourStopDto(
                stop.getPosition(),
                stop.getKilometers(),
                puntuadas.contains(stop.getBurgerJoint().getId()),
                fichaDeLocal.conDeseo(stop.getBurgerJoint(), false)))
            .toList();

        return new SavedTourDto(tour.getId(), tour.getName(), tour.getKilometers(),
            tour.getMinutes(), tour.getTravelMode(), tour.getCreatedAt(), paradas);
    }
}
