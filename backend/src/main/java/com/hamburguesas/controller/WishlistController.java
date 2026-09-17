package com.hamburguesas.controller;

import com.hamburguesas.dto.HamburgueseriaDto;
import com.hamburguesas.security.CurrentUser;
import com.hamburguesas.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    public List<HamburgueseriaDto> listar() {
        return wishlistService.listar(CurrentUser.idRequerido());
    }

    @PostMapping("/{hamburgueseriaId}")
    public ResponseEntity<Void> agregar(@PathVariable Long hamburgueseriaId) {
        wishlistService.agregar(CurrentUser.idRequerido(), hamburgueseriaId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{hamburgueseriaId}")
    public ResponseEntity<Void> quitar(@PathVariable Long hamburgueseriaId) {
        wishlistService.quitar(CurrentUser.idRequerido(), hamburgueseriaId);
        return ResponseEntity.noContent().build();
    }
}
