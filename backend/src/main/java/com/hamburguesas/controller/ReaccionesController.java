package com.hamburguesas.controller;

import com.hamburguesas.dto.ReaccionRequest;
import com.hamburguesas.dto.ReaccionesDto;
import com.hamburguesas.security.CurrentUser;
import com.hamburguesas.service.ReaccionesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tu reacción a una reseña (#186). Pide sesión: una reacción es de alguien.
 *
 * Va por el id de la reseña y no por el local, como el resto de las reseñas: acá no se
 * habla de la tuya en ese local, sino de una de otra persona.
 */
@RestController
@RequestMapping("/api/resenias/{ratingId}/reaccion")
@RequiredArgsConstructor
public class ReaccionesController {

    private final ReaccionesService reaccionesService;

    /** PUT porque poner la misma reacción dos veces deja todo igual. */
    @PutMapping
    public ReaccionesDto reaccionar(@PathVariable Long ratingId, @Valid @RequestBody ReaccionRequest pedido) {
        return reaccionesService.reaccionar(ratingId, CurrentUser.requireId(), pedido.tipo());
    }

    @DeleteMapping
    public ReaccionesDto sacar(@PathVariable Long ratingId) {
        return reaccionesService.sacar(ratingId, CurrentUser.requireId());
    }
}
