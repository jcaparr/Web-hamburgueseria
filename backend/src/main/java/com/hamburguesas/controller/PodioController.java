package com.hamburguesas.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.hamburguesas.podio.Podio;
import com.hamburguesas.podio.PodioDto;
import com.hamburguesas.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

/** El podio de personas de quien tiene la sesión (#318). Pide sesión. */
@RestController
@RequestMapping("/api/podio")
@RequiredArgsConstructor
public class PodioController {

    private static final Set<String> PERIODOS = Set.of(Podio.SEMANA, Podio.MES, Podio.GENERAL);

    private final Podio podio;

    @GetMapping
    public PodioDto podio(@RequestParam(defaultValue = Podio.SEMANA) String periodo) {
        if (!PERIODOS.contains(periodo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ese período no existe");
        }
        return podio.de(periodo, CurrentUser.requireId());
    }
}
