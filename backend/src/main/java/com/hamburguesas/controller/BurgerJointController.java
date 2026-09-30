package com.hamburguesas.controller;

import com.hamburguesas.dto.BurgerJointDto;
import com.hamburguesas.security.CurrentUser;
import com.hamburguesas.service.BurgerJointService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/burger-joints")
@RequiredArgsConstructor
public class BurgerJointController {

    private final BurgerJointService burgerJointService;

    /**
     * @param conCadenas si incluir las cadenas de comida rápida. Por omisión sí, que es
     *                   lo que se veía antes: apagarlas es una decisión de quien mira.
     * @param area el barrio, tal como lo devuelve /barrios. Sin barrio se ve la Ciudad
     *             entera, que es como estaba.
     */
    @GetMapping
    public Page<BurgerJointDto> search(
        @RequestParam(required = false) String q,
        @RequestParam(required = false) String area,
        @RequestParam(required = false, defaultValue = "true") boolean conCadenas,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        return burgerJointService.search(q, area, conCadenas, CurrentUser.idOrNull(), pageable);
    }

    /**
     * Los barrios que tienen al menos una hamburguesería.
     *
     * Vivía en el controlador de tours, que era el único que lo usaba. Es una propiedad
     * de los locales y no del recorrido, y ahora la piden los dos.
     *
     * No choca con /{id}: Spring prefiere el tramo literal, y además el id es un número.
     */
    @GetMapping("/barrios")
    public List<String> barrios() {
        return burgerJointService.barrios();
    }

    @GetMapping("/{id}")
    public BurgerJointDto get(@PathVariable Long id) {
        return burgerJointService.get(id, CurrentUser.idOrNull());
    }
}
