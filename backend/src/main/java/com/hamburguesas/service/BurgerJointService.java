package com.hamburguesas.service;

import com.hamburguesas.dto.BurgerJointDto;
import com.hamburguesas.exception.ResourceNotFoundException;
import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.repository.BurgerJointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BurgerJointService {

    private final BurgerJointRepository burgerJointRepository;
    private final FichaDeLocal fichaDeLocal;

    /**
     * El listado de Explorar.
     *
     * Los tres filtros son independientes y se combinan: se puede buscar un nombre
     * dentro de un barrio, con las cadenas apagadas. El que no viene no filtra.
     */
    public Page<BurgerJointDto> search(String query, List<String> areas, boolean conCadenas,
                                       Long userId, Pageable pageable) {
        return burgerJointRepository.buscar(query, areas, conCadenas, pageable)
            .map(b -> fichaDeLocal.para(b, userId));
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
}
