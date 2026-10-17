package com.hamburguesas.controller;

import com.hamburguesas.dto.SugerenciaDeLocalRequest;
import com.hamburguesas.security.CurrentUser;
import com.hamburguesas.service.SugerenciasDeLocales;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/** "Sugerir un local" (#257). Pide sesión: cae en el anyRequest().authenticated(). */
@RestController
@RequestMapping("/api/sugerencias")
@RequiredArgsConstructor
public class SugerenciasController {

    private final SugerenciasDeLocales sugerenciasDeLocales;

    /**
     * En multipart porque puede traer una foto (#283), en el campo "foto". Los demás
     * datos van sueltos, como en las reseñas.
     */
    @PostMapping
    public ResponseEntity<Void> sugerir(
        @Valid @ModelAttribute SugerenciaDeLocalRequest pedido,
        @RequestParam(value = "foto", required = false) MultipartFile foto
    ) throws IOException {
        sugerenciasDeLocales.sugerir(CurrentUser.requireId(), pedido,
            foto == null || foto.isEmpty() ? null : foto.getBytes());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
