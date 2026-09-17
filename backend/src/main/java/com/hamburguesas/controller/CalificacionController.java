package com.hamburguesas.controller;

import com.hamburguesas.dto.CalificacionRequest;
import com.hamburguesas.dto.CalificacionResponse;
import com.hamburguesas.security.CurrentUser;
import com.hamburguesas.service.CalificacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hamburguesuerias/{hamburgueseriaId}/calificaciones")
@RequiredArgsConstructor
public class CalificacionController {

    private final CalificacionService calificacionService;

    @GetMapping
    public Page<CalificacionResponse> listar(
        @PathVariable Long hamburgueseriaId,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        return calificacionService.listar(hamburgueseriaId, pageable);
    }

    @PostMapping
    public ResponseEntity<CalificacionResponse> calificar(
        @PathVariable Long hamburgueseriaId,
        @Valid @RequestBody CalificacionRequest request
    ) {
        CalificacionResponse response = calificacionService.calificar(
            CurrentUser.idRequerido(), hamburgueseriaId, request
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping
    public CalificacionResponse editar(
        @PathVariable Long hamburgueseriaId,
        @Valid @RequestBody CalificacionRequest request
    ) {
        return calificacionService.editar(CurrentUser.idRequerido(), hamburgueseriaId, request);
    }
}
