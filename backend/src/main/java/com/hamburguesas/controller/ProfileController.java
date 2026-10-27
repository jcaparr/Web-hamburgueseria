package com.hamburguesas.controller;

import com.hamburguesas.dto.AuthResponse;
import com.hamburguesas.dto.HamburguesaRequest;
import com.hamburguesas.dto.MostrarListasDto;
import com.hamburguesas.dto.ProfileStatsDto;
import com.hamburguesas.dto.ReseniaDePerfilDto;
import com.hamburguesas.security.CurrentUser;
import com.hamburguesas.service.ListasDelPerfil;
import com.hamburguesas.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final ListasDelPerfil listasDelPerfil;
    private final com.hamburguesas.logros.Logros logros;

    @GetMapping("/stats")
    public ProfileStatsDto stats() {
        return profileService.stats(CurrentUser.requireId());
    }

    @GetMapping("/ratings")
    public List<ReseniaDePerfilDto> ratings() {
        return profileService.myRatings(CurrentUser.requireId());
    }

    /** La hamburguesa de tu avatar. Con receta null vuelve a la que sale de tu nombre. */
    @PutMapping("/hamburguesa")
    public AuthResponse hamburguesa(@Valid @RequestBody HamburguesaRequest pedido) {
        return profileService.cambiarHamburguesa(CurrentUser.requireId(), pedido.receta());
    }

    /** Elige el logro que se muestra en tu perfil, o lo saca con null (#319). */
    @PutMapping("/logro-exhibido")
    public AuthResponse logroExhibido(@RequestBody com.hamburguesas.dto.LogroExhibidoRequest pedido) {
        return com.hamburguesas.auth.SessionIssuer.quienEs(logros.exhibir(CurrentUser.requireId(), pedido.logro()));
    }

    /** Si mostrás tus guardadas y tus recorridos en tu perfil público (#264). */
    @GetMapping("/muestra-listas")
    public MostrarListasDto muestraListas() {
        return new MostrarListasDto(listasDelPerfil.lasMuestra(CurrentUser.requireId()));
    }

    @PutMapping("/muestra-listas")
    public MostrarListasDto mostrarListas(@Valid @RequestBody MostrarListasDto pedido) {
        return new MostrarListasDto(listasDelPerfil.mostrar(CurrentUser.requireId(), pedido.muestra()));
    }
}
