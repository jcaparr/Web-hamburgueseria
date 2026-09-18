package com.hamburguesas.controller;

import com.hamburguesas.dto.MyRatingDto;
import com.hamburguesas.dto.ProfileStatsDto;
import com.hamburguesas.security.CurrentUser;
import com.hamburguesas.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/stats")
    public ProfileStatsDto stats() {
        return profileService.stats(CurrentUser.requireId());
    }

    @GetMapping("/ratings/recent")
    public List<MyRatingDto> recentRatings() {
        return profileService.recentRatings(CurrentUser.requireId());
    }
}
