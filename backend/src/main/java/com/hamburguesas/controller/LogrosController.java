package com.hamburguesas.controller;

import com.hamburguesas.logros.Logros;
import com.hamburguesas.logros.LogrosVistos;
import com.hamburguesas.logros.NovedadesDeLogros;
import com.hamburguesas.logros.PantallaDeLogros;
import com.hamburguesas.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Los logros de quien tiene la sesión (#316). Todo pide sesión. */
@RestController
@RequestMapping("/api/logros")
@RequiredArgsConstructor
public class LogrosController {

    private final Logros logros;

    /** Nivel, XP y cada logro con su avance. Antes de responder, recalcula. */
    @GetMapping
    public PantallaDeLogros mios() {
        return logros.pantalla(CurrentUser.requireId());
    }

    /** Lo que hay para avisar al ganar un logro (#317). No marca nada como visto. */
    @GetMapping("/novedades")
    public NovedadesDeLogros novedades() {
        return logros.novedades(CurrentUser.requireId());
    }

    /** Lo llama la pantalla después de mostrar el aviso. */
    @PostMapping("/vistos")
    public ResponseEntity<Void> vistos(@RequestBody LogrosVistos vistos) {
        logros.marcarVistos(CurrentUser.requireId(), vistos);
        return ResponseEntity.noContent().build();
    }
}
