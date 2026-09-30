package com.hamburguesas.service;

import com.hamburguesas.dto.ProfileStatsDto;
import com.hamburguesas.dto.ReseniaDePerfilDto;
import com.hamburguesas.repository.FollowRepository;
import com.hamburguesas.repository.RatingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final RatingRepository ratingRepository;
    private final FollowRepository followRepository;

    public ProfileStatsDto stats(Long userId) {
        long ratingsCount = ratingRepository.countByUser_Id(userId);
        Double averageScore = ratingRepository.averageScoreByUser(userId);
        return new ProfileStatsDto(
            ratingsCount, followRepository.countByFollowed_Id(userId), averageScore);
    }

    public List<ReseniaDePerfilDto> myRatings(Long userId) {
        return ratingRepository.findAllByUserOrderByCreatedAtDesc(userId);
    }
}
