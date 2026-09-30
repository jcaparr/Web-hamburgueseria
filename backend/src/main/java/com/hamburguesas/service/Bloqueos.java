package com.hamburguesas.service;

import com.hamburguesas.repository.BlockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * A quién no le tiene que aparecer quién.
 *
 * Vive aparte porque la misma pregunta la hacen dos pantallas que no se conocen entre
 * sí —el feed y el buscador— y la respuesta tiene que ser la misma en las dos. Un
 * bloqueo que esconde a alguien del feed pero lo deja salir en la búsqueda no es un
 * bloqueo.
 */
@Component
@RequiredArgsConstructor
public class Bloqueos {

    /**
     * Un id que ninguna fila puede tener, para cuando no hay a nadie que esconder.
     *
     * Las consultas filtran con "not in", y "not in ()" no es SQL válido: la lista
     * nunca puede volver vacía. La alternativa sería tener dos versiones de cada
     * consulta, una con el filtro y otra sin él.
     */
    private static final long NADIE = -1L;

    private final BlockRepository blockRepository;

    /** Con quiénes hay un bloqueo de por medio, lo haya puesto cualquiera de los dos. */
    public List<Long> queNoPuedeVer(Long usuarioId) {
        if (usuarioId == null) {
            return List.of(NADIE);
        }
        List<Long> ocultos = new ArrayList<>(blockRepository.idsQueNoPuedeVer(usuarioId));
        return ocultos.isEmpty() ? List.of(NADIE) : ocultos;
    }

    /** Lo mismo, más uno mismo: nadie se busca a sí mismo en el buscador de gente. */
    public List<Long> queNoPuedeVerNiASiMismo(Long usuarioId) {
        if (usuarioId == null) {
            return List.of(NADIE);
        }
        List<Long> ocultos = new ArrayList<>(blockRepository.idsQueNoPuedeVer(usuarioId));
        ocultos.add(usuarioId);
        return ocultos;
    }

    public boolean hayEntre(Long uno, Long otro) {
        return uno != null && blockRepository.hayBloqueoEntre(uno, otro);
    }
}
