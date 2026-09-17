package com.hamburguesas.controller;

import com.hamburguesas.dto.HamburgueseriaDto;
import com.hamburguesas.security.CurrentUser;
import com.hamburguesas.service.HamburgueseriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hamburguesuerias")
@RequiredArgsConstructor
public class HamburgueseriaController {

    private final HamburgueseriaService hamburgueseriaService;

    @GetMapping
    public Page<HamburgueseriaDto> buscar(
        @RequestParam(required = false) String q,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        return hamburgueseriaService.buscar(q, CurrentUser.idOrNull(), pageable);
    }

    @GetMapping("/{id}")
    public HamburgueseriaDto obtener(@PathVariable Long id) {
        return hamburgueseriaService.obtener(id, CurrentUser.idOrNull());
    }
}
