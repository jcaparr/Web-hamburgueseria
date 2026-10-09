package com.hamburguesas.service;

import com.hamburguesas.dto.BuzonDto;
import com.hamburguesas.dto.NotificacionDto;
import com.hamburguesas.dto.NotificacionesNuevasDto;
import com.hamburguesas.exception.ResourceNotFoundException;
import com.hamburguesas.model.Follow;
import com.hamburguesas.model.Reaccion;
import com.hamburguesas.model.User;
import com.hamburguesas.repository.FollowRepository;
import com.hamburguesas.repository.ReaccionRepository;
import com.hamburguesas.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * El buzón de notificaciones: quién te siguió y quién reaccionó a tus reseñas (#210).
 *
 * No tiene tabla propia. Se arma cada vez con los seguimientos y las reacciones que ya
 * están guardados, cada uno con su fecha, y lo único que se anota es cuándo lo abrió
 * cada uno por última vez. Así se mantiene solo: si alguien te deja de seguir o saca su
 * reacción, el aviso desaparece; si hay un bloqueo de por medio, no se ve. Una tabla de
 * avisos habría que limpiarla en cada uno de esos casos.
 */
@Service
@RequiredArgsConstructor
public class NotificacionesService {

    /**
     * Cuántos avisos muestra el buzón. Es un buzón y no un historial: lo de hace meses no
     * le sirve a nadie, y así la pantalla no crece sin límite.
     */
    static final int CUANTAS = 50;

    private final FollowRepository followRepository;
    private final ReaccionRepository reaccionRepository;
    private final UserRepository userRepository;
    private final Bloqueos bloqueos;

    /**
     * Los avisos más recientes, mezclados y por fecha.
     *
     * Marca cuáles son nuevos pero no los da por vistos: eso lo pide la pantalla aparte,
     * después de mostrarlos. Si se marcaran acá, un error al dibujarlos los perdería.
     */
    @Transactional(readOnly = true)
    public BuzonDto buzon(Long yo) {
        Instant vistas = desdeCuandoSonNuevas(persona(yo));
        List<Long> ocultos = bloqueos.queNoPuedeVer(yo);
        PageRequest primeras = PageRequest.of(0, CUANTAS);

        List<Follow> seguimientos = followRepository.seguimientosA(yo, ocultos, primeras);
        List<Reaccion> reacciones = reaccionRepository.aLasReseniasDe(yo, ocultos, primeras);
        Set<Long> sigo = aCualesSigue(yo, seguimientos);

        List<NotificacionDto> avisos = Stream.concat(
                seguimientos.stream().map(f -> deSeguimiento(f, vistas, sigo)),
                reacciones.stream().map(r -> deReaccion(r, vistas)))
            .sorted(Comparator.comparing(NotificacionDto::cuando).reversed())
            .limit(CUANTAS)
            .toList();

        return new BuzonDto(avisos, avisos.stream().filter(NotificacionDto::nueva).count());
    }

    /**
     * Cuántos avisos hay sin ver, para la campana. Se pide en cada cambio de pantalla, así
     * que va con dos conteos y no armando el buzón entero.
     */
    @Transactional(readOnly = true)
    public NotificacionesNuevasDto nuevas(Long yo) {
        Instant vistas = desdeCuandoSonNuevas(persona(yo));
        List<Long> ocultos = bloqueos.queNoPuedeVer(yo);
        return new NotificacionesNuevasDto(
            followRepository.seguimientosNuevosA(yo, ocultos, vistas)
                + reaccionRepository.nuevasALasReseniasDe(yo, ocultos, vistas));
    }

    /** Da por visto todo lo que llegó hasta ahora: la campana vuelve a cero. */
    @Transactional
    public void marcarVistas(Long yo) {
        persona(yo).setNotificacionesVistasEl(Instant.now());
    }

    private User persona(Long yo) {
        return userRepository.findById(yo)
            .orElseThrow(() -> new ResourceNotFoundException("No encontramos tu cuenta"));
    }

    /** Sin fecha es que nunca lo abrió, y entonces todo es nuevo. */
    private static Instant desdeCuandoSonNuevas(User persona) {
        return persona.getNotificacionesVistasEl() == null ? Instant.EPOCH : persona.getNotificacionesVistasEl();
    }

    private Set<Long> aCualesSigue(Long yo, List<Follow> seguimientos) {
        if (seguimientos.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(followRepository.idsQueSigueDeEntre(
            yo, seguimientos.stream().map(f -> f.getFollower().getId()).toList()));
    }

    private static NotificacionDto deSeguimiento(Follow seguimiento, Instant vistas, Set<Long> sigo) {
        User quien = seguimiento.getFollower();
        return new NotificacionDto(NotificacionDto.Tipo.SEGUIMIENTO, seguimiento.getCreatedAt(),
            seguimiento.getCreatedAt().isAfter(vistas), quien.getId(), quien.getUsername(),
            quien.getHamburguesa(), sigo.contains(quien.getId()), null, null, null);
    }

    private static NotificacionDto deReaccion(Reaccion reaccion, Instant vistas) {
        User quien = reaccion.getUser();
        var local = reaccion.getRating().getBurgerJoint();
        return new NotificacionDto(NotificacionDto.Tipo.REACCION, reaccion.getCreatedAt(),
            reaccion.getCreatedAt().isAfter(vistas), quien.getId(), quien.getUsername(),
            quien.getHamburguesa(), false, reaccion.getTipo(), local.getId(), local.getName());
    }
}
