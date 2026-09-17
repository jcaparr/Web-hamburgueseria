package com.hamburguesas.controller;

import com.hamburguesas.dto.RankingItemDto;
import com.hamburguesas.security.CurrentUser;
import com.hamburguesas.service.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ranking")
@RequiredArgsConstructor
public class RankingController {

    private final RankingService rankingService;

    @GetMapping("/general")
    public Page<RankingItemDto> general(
        @RequestParam(required = false) String area,
        @RequestParam(required = false, defaultValue = "score") String order,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        return rankingService.generalRanking(area, order, pageable);
    }

    @GetMapping("/mine")
    public Page<RankingItemDto> mine(@PageableDefault(size = 20) Pageable pageable) {
        return rankingService.personalRanking(CurrentUser.requireId(), pageable);
    }
}
