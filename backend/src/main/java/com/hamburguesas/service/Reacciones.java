package com.hamburguesas.service;

import com.hamburguesas.dto.CuantasReaccionesDto;
import com.hamburguesas.dto.ReaccionesDto;
import com.hamburguesas.model.TipoDeReaccion;
import com.hamburguesas.repository.ReaccionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Las reacciones de varias reseñas, contadas, de una vez (#186).
 *
 * Vive aparte porque la piden dos pantallas que no se conocen —el feed y la ficha del
 * local— y las dos dibujan una página entera de reseñas: preguntar de a una serían
 * veinte consultas por página. Así son dos: las cantidades y las de quien mira.
 *
 * Cuenta todas, también las de gente con la que quien mira tiene un bloqueo. Es un
 * número, no una lista de nombres, y por lo mismo que el promedio de un local no
 * depende de a quién bloqueaste, las reacciones de una reseña tampoco.
 */
@Component
@RequiredArgsConstructor
public class Reacciones {

    private final ReaccionRepository reaccionRepository;

    /**
     * @param quienMira puede ser nulo: sin sesión se ven las cantidades y ninguna es "mía".
     * @return una entrada por cada reseña pedida, con {@link ReaccionesDto#NINGUNA} si no
     *   tiene ninguna.
     */
    public Map<Long, ReaccionesDto> de(Collection<Long> ids, Long quienMira) {
        if (ids.isEmpty()) {
            return Map.of();
        }

        Map<Long, Map<TipoDeReaccion, Long>> cuantas = new HashMap<>();
        for (Object[] fila : reaccionRepository.contarEn(ids)) {
            cuantas.computeIfAbsent((Long) fila[0], id -> new EnumMap<>(TipoDeReaccion.class))
                .put((TipoDeReaccion) fila[1], (Long) fila[2]);
        }

        Map<Long, TipoDeReaccion> mias = new HashMap<>();
        if (quienMira != null) {
            for (Object[] fila : reaccionRepository.deUnoEn(quienMira, ids)) {
                mias.put((Long) fila[0], (TipoDeReaccion) fila[1]);
            }
        }

        Map<Long, ReaccionesDto> porResenia = new HashMap<>();
        for (Long id : ids) {
            Map<TipoDeReaccion, Long> deEsta = cuantas.get(id);
            porResenia.put(id, deEsta == null
                ? ReaccionesDto.NINGUNA
                : new ReaccionesDto(ordenadas(deEsta), mias.get(id)));
        }
        return porResenia;
    }

    /** De la más usada a la menos; a igual cantidad, en el orden del selector. */
    private static List<CuantasReaccionesDto> ordenadas(Map<TipoDeReaccion, Long> cuantas) {
        return cuantas.entrySet().stream()
            .map(e -> new CuantasReaccionesDto(e.getKey(), e.getValue()))
            .sorted(Comparator.comparingLong(CuantasReaccionesDto::cuantas).reversed()
                .thenComparing(CuantasReaccionesDto::tipo))
            .toList();
    }
}
