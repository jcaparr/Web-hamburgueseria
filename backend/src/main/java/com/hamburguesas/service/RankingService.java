package com.hamburguesas.service;

import com.hamburguesas.dto.RankingItemDto;
import com.hamburguesas.repository.CalificacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RankingService {

    private final CalificacionRepository calificacionRepository;

    public Page<RankingItemDto> rankingGeneral(String zona, String orden, Pageable pageable) {
        if ("popularidad".equalsIgnoreCase(orden)) {
            return calificacionRepository.rankingPorPopularidad(zona, pageable);
        }
        return calificacionRepository.rankingPorPuntaje(zona, pageable);
    }

    public Page<RankingItemDto> rankingPersonal(Long usuarioId, Pageable pageable) {
        return calificacionRepository.rankingPersonal(usuarioId, pageable);
    }
}
