package com.hamburguesas.controller;

import com.hamburguesas.dto.PaginaDeFeedDto;
import com.hamburguesas.model.FuenteDelFeed;
import com.hamburguesas.security.CurrentUser;
import com.hamburguesas.service.FeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feed")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    /**
     * @param cursor por dónde seguir, tal como lo devolvió la llamada anterior. Vacío
     *   en la primera pantalla.
     */
    @GetMapping
    public PaginaDeFeedDto ver(
        @RequestParam(defaultValue = "TODOS") FuenteDelFeed fuente,
        @RequestParam(required = false) String cursor
    ) {
        return feedService.ver(fuente, cursor, CurrentUser.requireId());
    }
}
