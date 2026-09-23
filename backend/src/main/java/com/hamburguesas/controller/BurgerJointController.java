package com.hamburguesas.controller;

import com.hamburguesas.dto.BurgerJointDto;
import com.hamburguesas.security.CurrentUser;
import com.hamburguesas.service.BurgerJointService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/burger-joints")
@RequiredArgsConstructor
public class BurgerJointController {

    private final BurgerJointService burgerJointService;

    /**
     * @param conCadenas si incluir las cadenas de comida rápida. Por omisión sí, que es
     *                   lo que se veía antes: apagarlas es una decisión de quien mira.
     */
    @GetMapping
    public Page<BurgerJointDto> search(
        @RequestParam(required = false) String q,
        @RequestParam(required = false, defaultValue = "true") boolean conCadenas,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        return burgerJointService.search(q, conCadenas, CurrentUser.idOrNull(), pageable);
    }

    @GetMapping("/{id}")
    public BurgerJointDto get(@PathVariable Long id) {
        return burgerJointService.get(id, CurrentUser.idOrNull());
    }
}
