package com.hamburguesas.controller;

import com.hamburguesas.dto.GuardarTourRequest;
import com.hamburguesas.dto.SavedTourDto;
import com.hamburguesas.dto.TourDto;
import com.hamburguesas.model.ModoDeViaje;
import com.hamburguesas.repository.BurgerJointRepository;
import com.hamburguesas.security.CurrentUser;
import com.hamburguesas.service.SavedTourService;
import com.hamburguesas.service.TourService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * Arma recorridos para salir a comer hamburguesas, y guarda los que alguien quiso.
 *
 * Armarlo funciona sin sesión; guardarlo no, porque se guarda en el perfil de alguien.
 * Con sesión, armar uno además puede dejar afuera las que ya se probaron y los
 * recorridos que ya están guardados.
 */
@RestController
@RequestMapping("/api/tours")
@RequiredArgsConstructor
public class TourController {

    private final TourService tourService;
    private final SavedTourService savedTourService;
    private final BurgerJointRepository burgerJointRepository;

    /**
     * @param barrios           en cuáles buscar. Vacío significa alrededor de quien lo pide.
     * @param latitud           dónde está quien lo pide, si lo compartió.
     * @param kilometrosMaximos cuánto está dispuesto a recorrer en total.
     * @param modo              a pie o en auto, que cambia las vueltas y lo que se tarda.
     * @param excluirLasDeMisTours  dejar afuera las hamburgueserías por las que esta
     *                          persona ya pasó en sus recorridos guardados.
     * @param distintoDe        los recorridos ya propuestos en esta vuelta, cada uno como
     *                          sus ids unidos por guión: distintoDe=1-2-3&distintoDe=2-3-4.
     *                          Van todos y no solo el último porque con uno solo, pedir
     *                          otro alternaba entre dos: el nuevo excluía al viejo, y el
     *                          siguiente volvía al viejo porque ya no estaba prohibido.
     * @param semilla           fija el sorteo. La pantalla no la manda —quiere algo
     *                          distinto cada vez—; sirve para poder afirmar el resultado.
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
        @RequestParam(defaultValue = "A_PIE") ModoDeViaje modo,
        @RequestParam(defaultValue = "true") boolean excluirLasDeMisTours,
        @RequestParam(required = false) List<String> distintoDe,
        @RequestParam(required = false) Long semilla
    ) {
        var pedido = new TourService.Pedido(cantidad, kilometrosMaximos, barrios,
            latitud, longitud, incluirVisitadas, conCadenas, modo,
            excluirLasDeMisTours, comoCombinaciones(distintoDe), semilla);

        return tourService.armar(pedido, CurrentUser.idOrNull());
    }

    /**
     * "1-2-3" y "2-3-4" como dos recorridos.
     *
     * Unidos por guión y no por coma porque Spring parte por coma al armar una lista, y
     * los dos recorridos llegarían como seis ids sueltos, perdiendo qué va con qué.
     */
    private static List<List<Long>> comoCombinaciones(List<String> crudos) {
        if (crudos == null) {
            return List.of();
        }
        return crudos.stream().map(TourController::comoIds).filter(ids -> !ids.isEmpty()).toList();
    }

    private static List<Long> comoIds(String crudo) {
        List<Long> ids = new ArrayList<>();
        for (String parte : crudo.split("-")) {
            try {
                ids.add(Long.valueOf(parte.trim()));
            } catch (NumberFormatException ignorada) {
                // Un id que no es un número no señala ningún recorrido: se ignora en vez
                // de contestar un error por algo que solo empeora la sugerencia.
            }
        }
        return ids;
    }

    /** Los barrios que tienen al menos una hamburguesería, para el selector. */
    @GetMapping("/barrios")
    public List<String> barrios() {
        return burgerJointRepository.barriosConLocales();
    }

    @PostMapping
    public ResponseEntity<SavedTourDto> guardar(@Valid @RequestBody GuardarTourRequest request) {
        SavedTourDto guardado = savedTourService.guardar(CurrentUser.requireId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(guardado);
    }

    @GetMapping("/mios")
    public List<SavedTourDto> mios() {
        return savedTourService.mios(CurrentUser.requireId());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> borrar(@PathVariable Long id) {
        savedTourService.borrar(CurrentUser.requireId(), id);
        return ResponseEntity.noContent().build();
    }
}
