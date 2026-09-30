package com.hamburguesas.controller;

import com.hamburguesas.dto.RatingRequest;
import com.hamburguesas.dto.RatingResponse;
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
        return ratingService.list(burgerJointId, pageable);
    }

    @PostMapping
    public ResponseEntity<RatingResponse> rate(
        @PathVariable Long burgerJointId,
        @Valid @RequestBody RatingRequest request
    ) {
        RatingResponse response = ratingService.rate(
            CurrentUser.requireId(), burgerJointId, request
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping
    public RatingResponse update(
        @PathVariable Long burgerJointId,
        @Valid @RequestBody RatingRequest request
    ) {
        return ratingService.update(CurrentUser.requireId(), burgerJointId, request);
    }

    /**
     * La foto va aparte del texto, en su propia llamada.
     *
     * Podría viajar todo junto en un multipart, pero entonces un problema con la foto
     * —que pesa, que tarda, que no se pudo leer— se llevaría puesto lo escrito. Así, si
     * la foto falla, la reseña ya está guardada y lo único que hay que reintentar es la
     * foto. Por eso también exige que la reseña exista: primero se opina, después se
     * ilustra.
     */
    @PutMapping("/foto")
    public RatingResponse subirFoto(
        @PathVariable Long burgerJointId,
        @RequestParam("foto") MultipartFile foto
    ) {
        return ratingService.guardarFoto(CurrentUser.requireId(), burgerJointId, foto);
    }

    @DeleteMapping("/foto")
    public RatingResponse borrarFoto(@PathVariable Long burgerJointId) {
        return ratingService.borrarFoto(CurrentUser.requireId(), burgerJointId);
    }
}
