package com.hamburguesas.service;

import com.hamburguesas.dto.BurgerJointDto;
import com.hamburguesas.dto.HorarioDto;
import com.hamburguesas.exception.ResourceNotFoundException;
import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.repository.BurgerJointRepository;
import com.hamburguesas.repository.OrdenDeLocales;
import com.hamburguesas.repository.FranjaHorariaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BurgerJointService {

    private final BurgerJointRepository burgerJointRepository;
    private final FichaDeLocal fichaDeLocal;
    private final FranjaHorariaRepository franjaRepository;

    /**
     * El listado de Explorar.
     *
     * Los tres filtros son independientes y se combinan: se puede buscar un nombre
     * dentro de un barrio, con las cadenas apagadas. El que no viene no filtra.
     */
    public Page<BurgerJointDto> search(String query, List<String> areas, boolean conCadenas,
                                       OrdenDeLocales orden, Long userId, Pageable pageable) {
        Page<BurgerJoint> pagina = burgerJointRepository.buscar(query, areas, conCadenas, orden, pageable);
        // Las fichas de toda la página juntas: de a una eran dos o tres consultas por
        // local (#101).
        List<BurgerJointDto> fichas = fichaDeLocal.para(pagina.getContent(), userId);
        return new PageImpl<>(fichas, pagina.getPageable(), pagina.getTotalElements());
    }

    /** Los barrios que tienen al menos una hamburguesería, para el selector. */
    public List<String> barrios() {
        return burgerJointRepository.barriosConLocales();
    }

    public BurgerJointDto get(Long id, Long userId) {
        BurgerJoint burgerJoint = burgerJointRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Burger joint not found"));
        return fichaDeLocal.para(burgerJoint, userId);
    }

    /**
     * El horario del local, aparte de su ficha.
     *
     * Aparte porque la ficha es la misma que se usa en los listados, y Explorar no
     * necesita el horario de veinte locales para dibujar veinte tarjetas.
     */
    public HorarioDto horario(Long id) {
        BurgerJoint burgerJoint = burgerJointRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Burger joint not found"));

        List<HorarioDto.Franja> franjas = franjaRepository
            .findByBurgerJoint_IdOrderByDiaAscAbreAsc(id).stream()
            .map(f -> new HorarioDto.Franja(f.getDia(), f.getAbre(), f.getCierra()))
            .toList();
        return new HorarioDto(franjas, burgerJoint.getHorarioConsultadoEl() != null);
    }
}
