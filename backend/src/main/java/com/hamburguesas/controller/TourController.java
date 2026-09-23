package com.hamburguesas.controller;

import com.hamburguesas.dto.TourDto;
import com.hamburguesas.repository.BurgerJointRepository;
import com.hamburguesas.security.CurrentUser;
import com.hamburguesas.service.TourService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Arma recorridos para salir a comer hamburguesas.
 *
 * Sin sesión funciona igual: lo único que cambia con la sesión es poder dejar afuera las
 * que ya se probaron, que son las que esa persona puntuó.
 */
@RestController
@RequestMapping("/api/tours")
@RequiredArgsConstructor
public class TourController {

    private final TourService tourService;
    private final BurgerJointRepository burgerJointRepository;

    /**
     * @param barrios           en cuáles buscar. Vacío significa alrededor de quien camina.
     * @param latitud           dónde está quien camina, si lo compartió.
     * @param kilometrosMaximos cuánto está dispuesto a caminar en todo el recorrido.
     * @param semilla           para que volver a pedirlo proponga otro recorrido. La manda
     *                          la pantalla en cada intento; fija, el recorrido se repite.
     */
    @GetMapping
    public TourDto armar(
        @RequestParam(defaultValue = "4") int cantidad,
        @RequestParam(required = false) Double kilometrosMaximos,
        @RequestParam(required = false) List<String> barrios,
        @RequestParam(required = false) Double latitud,
        @RequestParam(required = false) Double longitud,
        @RequestParam(defaultValue = "true") boolean incluirVisitadas,
        @RequestParam(defaultValue = "false") boolean conCadenas,
        @RequestParam(required = false) Long semilla
    ) {
        var pedido = new TourService.Pedido(cantidad, kilometrosMaximos, barrios,
            latitud, longitud, incluirVisitadas, conCadenas, semilla);

        return tourService.armar(pedido, CurrentUser.idOrNull());
    }

    /** Los barrios que tienen al menos una hamburguesería, para el selector. */
    @GetMapping("/barrios")
    public List<String> barrios() {
        return burgerJointRepository.barriosConLocales();
    }
}
