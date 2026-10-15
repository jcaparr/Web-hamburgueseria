package com.hamburguesas.controller;

import com.hamburguesas.dto.SugerenciaDeLocalRequest;
import com.hamburguesas.security.CurrentUser;
import com.hamburguesas.service.SugerenciasDeLocales;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** "Sugerir un local" (#257). Pide sesión: cae en el anyRequest().authenticated(). */
@RestController
@RequestMapping("/api/sugerencias")
@RequiredArgsConstructor
public class SugerenciasController {

    private final SugerenciasDeLocales sugerenciasDeLocales;

    @PostMapping
    public ResponseEntity<Void> sugerir(@Valid @RequestBody SugerenciaDeLocalRequest pedido) {
        sugerenciasDeLocales.sugerir(CurrentUser.requireId(), pedido);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
