package com.hamburguesas.service;

import com.hamburguesas.dto.BurgerJointDto;
import com.hamburguesas.dto.HorarioDto;
import com.hamburguesas.exception.ResourceNotFoundException;
import com.hamburguesas.geo.Ubicacion;
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
     * El nombre y los barrios se combinan: se puede buscar un nombre dentro de un
     * barrio. El que no viene no filtra. Las cadenas no salen nunca (#206).
     */
    public Page<BurgerJointDto> search(String query, List<String> areas, OrdenDeLocales orden,
                                       Long userId, Pageable pageable) {
        return search(query, areas, orden, null, userId, pageable);
    }

    /**
     * Con la ubicación de quien mira, para el orden por cercanía (#288). Se usa solo
     * para ordenar: no se guarda ni se le pregunta nada a Google.
     */
    public Page<BurgerJointDto> search(String query, List<String> areas, OrdenDeLocales orden,
                                       Ubicacion ubicacion, Long userId, Pageable pageable) {
        return search(query, areas, orden, ubicacion, false, userId, pageable);
    }

    /** Con {@code paraResenia}, también las sucursales de cadenas (#335). */
    public Page<BurgerJointDto> search(String query, List<String> areas, OrdenDeLocales orden,
                                       Ubicacion ubicacion, boolean paraResenia, Long userId, Pageable pageable) {
        Page<BurgerJoint> pagina = burgerJointRepository.buscar(query, areas, orden, ubicacion, paraResenia, pageable);
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

    public BurgerJointDto getPorSlug(String slug, Long userId) {
        BurgerJoint burgerJoint = burgerJointRepository.findBySlug(slug)
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
