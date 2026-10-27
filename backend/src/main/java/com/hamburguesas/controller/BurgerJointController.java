package com.hamburguesas.controller;

import com.hamburguesas.dto.BurgerJointDto;
import com.hamburguesas.dto.HorarioDto;
import com.hamburguesas.geo.Ubicacion;
import com.hamburguesas.repository.OrdenDeLocales;
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
     * El listado de Explorar, siempre sin las cadenas de comida rápida (#206): se ven
     * buscándolas por nombre, en /api/cadenas. Una dirección vieja con "?conCadenas=true"
     * sigue andando, porque el parámetro se ignora.
     *
     * @param area uno o varios barrios, tal como los devuelve /barrios, repitiendo el
     *             parámetro: "?area=Palermo&area=Belgrano". Varios se leen como "o".
     *             Sin ninguno se ve todo, que es como estaba.
     *             <p>
     *             Sigue llamándose "area" en singular aunque ahora acepte varios: es lo
     *             que hay escrito en las direcciones que la gente dejó en favoritos y en
     *             el historial del navegador, y uno solo sigue funcionando igual.
     * @param orden "relevantes" (por omisión), "mejores", "peores" o "cercanas"; ver
     *              OrdenDeLocales. Uno que no existe es el de por omisión, no un error.
     * @param lat   dónde está quien mira, para "cercanas" (#288). Solo ordena: no se
     * @param lon   guarda. Sin las dos, "cercanas" se ordena como "relevantes".
     * @param conCadenas "cadenas=true" suma las sucursales de cadenas (#335); lo pide la
     *                   pantalla de reseña. Explorar no lo manda y sigue sin verlas.
     */
    @GetMapping
    public Page<BurgerJointDto> search(
        @RequestParam(required = false) String q,
        @RequestParam(name = "area", required = false) List<String> areas,
        @RequestParam(required = false) String orden,
        @RequestParam(required = false) Double lat,
        @RequestParam(required = false) Double lon,
        @RequestParam(name = "cadenas", defaultValue = "false") boolean conCadenas,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        return burgerJointService.search(q, areas, OrdenDeLocales.de(orden), Ubicacion.de(lat, lon),
            conCadenas, CurrentUser.idOrNull(), Paginas.sinOrden(pageable));
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

    /** La ficha por su dirección linda (#305): la web la usa para abrir burgometro.com.ar/<slug>. */
    @GetMapping("/por-slug/{slug}")
    public BurgerJointDto getPorSlug(@PathVariable String slug) {
        return burgerJointService.getPorSlug(slug, CurrentUser.idOrNull());
    }

    /** Público como el resto del local: el horario se mira sin cuenta. */
    @GetMapping("/{id}/horario")
    public HorarioDto horario(@PathVariable Long id) {
        return burgerJointService.horario(id);
    }
}
