package com.hamburguesas.controller;

import com.hamburguesas.dto.PerfilPublicoDto;
import com.hamburguesas.dto.UsuarioBuscadoDto;
import com.hamburguesas.security.CurrentUser;
import com.hamburguesas.service.SocialService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Las otras personas de la app.
 *
 * Todo acá pide sesión, incluso lo que solo se lee. Un nombre de usuario es público
 * entre quienes están adentro, pero eso no es razón para que cualquiera de afuera
 * pueda recorrer el padrón entero de a veinte por vez.
 */
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuariosController {

    private final SocialService socialService;

    @GetMapping
    public List<UsuarioBuscadoDto> buscar(@RequestParam(defaultValue = "") String q) {
        return socialService.buscar(q, CurrentUser.requireId());
    }

    @GetMapping("/{username}")
    public PerfilPublicoDto perfil(@PathVariable String username) {
        return socialService.perfil(username, CurrentUser.requireId());
    }

    @PostMapping("/{username}/seguir")
    public ResponseEntity<Void> seguir(@PathVariable String username) {
        socialService.seguir(username, CurrentUser.requireId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{username}/seguir")
    public ResponseEntity<Void> dejarDeSeguir(@PathVariable String username) {
        socialService.dejarDeSeguir(username, CurrentUser.requireId());
        return ResponseEntity.noContent().build();
    }
}
