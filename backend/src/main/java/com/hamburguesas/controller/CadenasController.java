package com.hamburguesas.controller;

import com.hamburguesas.dto.CadenaDto;
import com.hamburguesas.dto.SucursalesDeCadenaDto;
import com.hamburguesas.security.CurrentUser;
import com.hamburguesas.service.CadenasService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Las cadenas de comida rápida, que en Explorar se ven solo buscándolas por nombre (#206). */
@RestController
@RequestMapping("/api/cadenas")
@RequiredArgsConstructor
public class CadenasController {

    private final CadenasService cadenasService;

    /**
     * @param q    lo que se escribió en el buscador de Explorar
     * @param area los barrios elegidos, repetidos como en /api/burger-joints
     */
    @GetMapping
    public List<CadenaDto> buscar(
        @RequestParam(required = false) String q,
        @RequestParam(name = "area", required = false) List<String> area
    ) {
        return cadenasService.buscar(q, area);
    }

    @GetMapping("/{marca}")
    public SucursalesDeCadenaDto sucursales(@PathVariable String marca) {
        return cadenasService.sucursales(marca, CurrentUser.idOrNull());
    }
}
