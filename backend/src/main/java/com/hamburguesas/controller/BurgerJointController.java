package com.hamburguesas.controller;

import com.hamburguesas.dto.BurgerJointDto;
import com.hamburguesas.dto.HorarioDto;
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
     * @param area uno o varios barrios, tal como los devuelve /barrios, repitiendo el
     *             parámetro: "?area=Palermo&area=Belgrano". Varios se leen como "o".
     *             Sin ninguno se ve todo, que es como estaba.
     *             <p>
     *             Sigue llamándose "area" en singular aunque ahora acepte varios: es lo
     *             que hay escrito en las direcciones que la gente dejó en favoritos y en
     *             el historial del navegador, y uno solo sigue funcionando igual.
     */
    @GetMapping
    public Page<BurgerJointDto> search(
        @RequestParam(required = false) String q,
        @RequestParam(name = "area", required = false) List<String> areas,
        @RequestParam(required = false, defaultValue = "true") boolean conCadenas,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        return burgerJointService.search(q, areas, conCadenas, CurrentUser.idOrNull(), pageable);
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

    /** Público como el resto del local: el horario se mira sin cuenta. */
    @GetMapping("/{id}/horario")
    public HorarioDto horario(@PathVariable Long id) {
        return burgerJointService.horario(id);
    }
}
