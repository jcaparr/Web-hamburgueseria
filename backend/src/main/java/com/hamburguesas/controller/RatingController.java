package com.hamburguesas.controller;

import com.hamburguesas.dto.RatingRequest;
import com.hamburguesas.dto.RatingResponse;
import com.hamburguesas.dto.ResumenDeReseniasDto;
import com.hamburguesas.security.CurrentUser;
import com.hamburguesas.service.RatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/burger-joints/{burgerJointId}/ratings")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    @GetMapping
    public Page<RatingResponse> list(
        @PathVariable Long burgerJointId,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        return ratingService.list(burgerJointId, CurrentUser.idOrNull(), Paginas.sinOrden(pageable));
    }

    /**
     * La distribución de notas y lo que dijeron los que seguís.
     *
     * Va aparte de la lista y no dentro de la ficha del local: la ficha se arma también
     * al listar veinte hamburgueserías en el explorar, y estas dos consultas ahí serían
     * cuarenta consultas para datos que esa pantalla no muestra.
     *
     * Responde sin sesión, con la parte social vacía.
     */
    @GetMapping("/resumen")
    public ResumenDeReseniasDto resumen(@PathVariable Long burgerJointId) {
        return ratingService.resumen(burgerJointId, CurrentUser.idOrNull());
    }

    /**
     * Texto y fotos viajan juntos, en multipart.
     *
     * Las fotos son parte de la reseña, no un agregado: mandarlas en otra llamada dejaría
     * abierta la ventana en que el texto ya está guardado y las fotos no, que es
     * exactamente la reseña sin foto que no queremos que exista.
     *
     * Los campos van sueltos y no como un JSON adentro del multipart: un formulario con
     * un archivo ya manda todo lo demás como campos, y meter JSON en el medio obligaría
     * a quien llama a armar dos codificaciones distintas en el mismo pedido.
     *
     * Cada foto va en un campo "foto", repetido: el mismo nombre que cuando había una
     * sola, así que la versión de la app de antes sigue andando.
     */
    @PostMapping
    public ResponseEntity<RatingResponse> rate(
        @PathVariable Long burgerJointId,
        @Valid @ModelAttribute RatingRequest request,
        @RequestParam(value = "foto", required = false) List<MultipartFile> fotos
    ) {
        RatingResponse response = ratingService.rate(
            CurrentUser.requireId(), burgerJointId, request, fotos
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Las fotos que ya tenía y se quedan van en "queda", repetido y en orden; las nuevas,
     * en "foto". Editar no es refotografiar: sin fotos nuevas, quedan las de antes.
     */
    @PutMapping
    public RatingResponse update(
        @PathVariable Long burgerJointId,
        @Valid @ModelAttribute RatingRequest request,
        @RequestParam(value = "foto", required = false) List<MultipartFile> fotos,
        @RequestParam(value = "queda", required = false) List<String> quedan
    ) {
        return ratingService.update(CurrentUser.requireId(), burgerJointId, request, fotos, quedan);
    }

    /** La reseña propia en este local, con su foto. No hay forma de nombrar la de otro. */
    @DeleteMapping
    public ResponseEntity<Void> borrar(@PathVariable Long burgerJointId) {
        ratingService.borrar(CurrentUser.requireId(), burgerJointId);
        return ResponseEntity.noContent().build();
    }
}
