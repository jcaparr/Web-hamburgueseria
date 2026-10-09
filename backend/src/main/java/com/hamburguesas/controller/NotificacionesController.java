package com.hamburguesas.controller;

import com.hamburguesas.dto.BuzonDto;
import com.hamburguesas.dto.NotificacionesNuevasDto;
import com.hamburguesas.security.CurrentUser;
import com.hamburguesas.service.NotificacionesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** El buzón de notificaciones de quien tiene la sesión (#210). Todo pide sesión. */
@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
public class NotificacionesController {

    private final NotificacionesService notificacionesService;

    @GetMapping
    public BuzonDto buzon() {
        return notificacionesService.buzon(CurrentUser.requireId());
    }

    /** Solo el número, para la campana. */
    @GetMapping("/nuevas")
    public NotificacionesNuevasDto nuevas() {
        return notificacionesService.nuevas(CurrentUser.requireId());
    }

    /** Lo llama la pantalla del buzón después de mostrarlo. */
    @PostMapping("/vistas")
    public ResponseEntity<Void> marcarVistas() {
        notificacionesService.marcarVistas(CurrentUser.requireId());
        return ResponseEntity.noContent().build();
    }
}
