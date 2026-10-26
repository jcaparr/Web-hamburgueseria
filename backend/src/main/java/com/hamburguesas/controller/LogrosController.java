package com.hamburguesas.controller;

import com.hamburguesas.logros.Logros;
import com.hamburguesas.logros.PantallaDeLogros;
import com.hamburguesas.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
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
}
