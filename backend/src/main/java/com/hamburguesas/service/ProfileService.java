package com.hamburguesas.service;

import com.hamburguesas.dto.MyRatingDto;
import com.hamburguesas.dto.ProfileStatsDto;
import com.hamburguesas.repository.RatingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

    public List<MyRatingDto> myRatings(Long userId) {
        return ratingRepository.findAllByUserOrderByCreatedAtDesc(userId);
    }
}
