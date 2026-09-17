package com.hamburguesas.service;

import com.hamburguesas.dto.HamburgueseriaDto;
import com.hamburguesas.exception.ResourceNotFoundException;
import com.hamburguesas.model.Hamburgueseria;
import com.hamburguesas.repository.CalificacionRepository;
import com.hamburguesas.repository.HamburgueseriaRepository;
import com.hamburguesas.repository.ListaDeseadosRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HamburgueseriaService {

    private final HamburgueseriaRepository hamburgueseriaRepository;
    private final CalificacionRepository calificacionRepository;
    private final ListaDeseadosRepository listaDeseadosRepository;

    public Page<HamburgueseriaDto> buscar(String query, Long usuarioId, Pageable pageable) {
        Page<Hamburgueseria> page = (query == null || query.isBlank())
            ? hamburgueseriaRepository.findAll(pageable)
            : hamburgueseriaRepository.findByNombreContainingIgnoreCase(query, pageable);

        return page.map(h -> toDto(h, usuarioId));
    }

    public HamburgueseriaDto obtener(Long id, Long usuarioId) {
        Hamburgueseria hamburgueseria = hamburgueseriaRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Hamburgueseria no encontrada"));
        return toDto(hamburgueseria, usuarioId);
    }

    private HamburgueseriaDto toDto(Hamburgueseria h, Long usuarioId) {
        Double promedio = calificacionRepository.promedioPorHamburgueseria(h.getId());
        long cantidad = calificacionRepository.countByHamburgueseria_Id(h.getId());
        boolean enWishlist = usuarioId != null &&
            listaDeseadosRepository.existsByUsuario_IdAndHamburgueseria_Id(usuarioId, h.getId());

        return new HamburgueseriaDto(
            h.getId(), h.getNombre(), h.getDireccion(), h.getZona(), h.getFotoUrl(),
            h.getLatitud(), h.getLongitud(),
            promedio, cantidad, enWishlist
        );
    }
}
