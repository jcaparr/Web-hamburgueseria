package com.hamburguesas.controller;

import com.hamburguesas.dto.AuthResponse;
import com.hamburguesas.dto.CambioDeNombreRequest;
import com.hamburguesas.dto.EstadoDelNombreDto;
import com.hamburguesas.dto.HamburguesaRequest;
import com.hamburguesas.dto.MostrarListasDto;
import com.hamburguesas.dto.ProfileStatsDto;
import com.hamburguesas.dto.ReseniaDePerfilDto;
import com.hamburguesas.security.CurrentUser;
import com.hamburguesas.service.CambioDeNombreDeUsuario;
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
    private final CambioDeNombreDeUsuario cambioDeNombre;
    private final com.hamburguesas.service.TuMes tuMes;


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

    /** Si ya puede cambiar su nombre de usuario o desde cuándo (#380). */
    @GetMapping("/username")
    public EstadoDelNombreDto estadoDelNombre() {
        return cambioDeNombre.estado(CurrentUser.requireId());
    }

    /** Cambia tu nombre de usuario, una vez cada 30 días (#380). */
    @PutMapping("/username")
    public AuthResponse cambiarNombre(@Valid @RequestBody CambioDeNombreRequest pedido) {
        return cambioDeNombre.cambiar(CurrentUser.requireId(), pedido.username());
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

    /**
     * El resumen de un mes, "Tu mes en Burgómetro" (#308). Sin parámetro, el del último mes
     * terminado; con {@code mes=2026-11}, ese. El mes que todavía corre no se resume.
     */
    @GetMapping("/tu-mes")
    public com.hamburguesas.dto.TuMesDto tuMes(@org.springframework.web.bind.annotation.RequestParam(required = false) String mes) {
        java.time.YearMonth pedido;
        try {
            pedido = mes == null ? null : java.time.YearMonth.parse(mes);
        } catch (java.time.format.DateTimeParseException e) {
            throw new com.hamburguesas.exception.ResourceNotFoundException("Ese mes no existe");
        }
        return tuMes.resumen(CurrentUser.requireId(), pedido);
    }

    /** Si hay un resumen de "Tu mes" para avisar hoy, y si ya vio el pop up (#405). */
    @GetMapping("/tu-mes/aviso")
    public com.hamburguesas.dto.AvisoDeTuMesDto avisoDeTuMes() {
        return tuMes.aviso(CurrentUser.requireId());
    }

    /** Anota que vio el pop up del resumen de hoy (#405). */
    @org.springframework.web.bind.annotation.PostMapping("/tu-mes/visto")
    public void tuMesVisto() {
        tuMes.marcarVisto(CurrentUser.requireId());
    }
}
