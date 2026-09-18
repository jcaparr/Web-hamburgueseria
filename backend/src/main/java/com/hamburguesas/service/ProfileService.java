package com.hamburguesas.service;

import com.hamburguesas.dto.MyRatingDto;
import com.hamburguesas.dto.ProfileStatsDto;
import com.hamburguesas.repository.RatingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final RatingRepository ratingRepository;

    public ProfileStatsDto stats(Long userId) {
        long ratingsCount = ratingRepository.countByUser_Id(userId);
        Double averageScore = ratingRepository.averageScoreByUser(userId);
        return new ProfileStatsDto(ratingsCount, averageScore);
    }

    public List<MyRatingDto> recentRatings(Long userId) {
        Instant since = Instant.now().minus(7, ChronoUnit.DAYS);
        return ratingRepository.findRecentByUser(userId, since);
    }
}
