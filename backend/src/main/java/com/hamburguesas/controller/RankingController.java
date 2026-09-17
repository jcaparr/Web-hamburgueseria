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
        @RequestParam(required = false) String zona,
        @RequestParam(required = false, defaultValue = "puntaje") String orden,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        return rankingService.rankingGeneral(zona, orden, pageable);
    }

    @GetMapping("/mio")
    public Page<RankingItemDto> mio(@PageableDefault(size = 20) Pageable pageable) {
        return rankingService.rankingPersonal(CurrentUser.idRequerido(), pageable);
    }
}
