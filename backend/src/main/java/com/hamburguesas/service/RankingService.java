package com.hamburguesas.service;

import com.hamburguesas.dto.RankingItemDto;
import com.hamburguesas.repository.RatingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RankingService {

    private final RatingRepository ratingRepository;

    public Page<RankingItemDto> generalRanking(String area, String order, Pageable pageable) {
        if ("popularity".equalsIgnoreCase(order)) {
            return ratingRepository.rankingByPopularity(area, pageable);
        }
        return ratingRepository.rankingByScore(area, pageable);
    }

    public Page<RankingItemDto> personalRanking(Long userId, Pageable pageable) {
        return ratingRepository.personalRanking(userId, pageable);
    }
}
