package com.hamburguesas.service;

import com.hamburguesas.auth.SessionIssuer;
import com.hamburguesas.dto.AuthResponse;
import com.hamburguesas.exception.ResourceNotFoundException;
import com.hamburguesas.exception.UsernameCooldownException;
import com.hamburguesas.exception.UsernameTakenException;
import com.hamburguesas.logros.Logros;
import com.hamburguesas.model.User;
import com.hamburguesas.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Cambiar el nombre de usuario, una vez cada 30 días (#380).
 *
 * Todo se decide acá y no en la pantalla: el frontend solo avisa. El nombre viejo queda
 * libre en el acto, sin redirección ni reserva.
 */
@Service
public class CambioDeNombreDeUsuario {

    public static final int DIAS_ENTRE_CAMBIOS = 30;

    private final UserRepository userRepository;
    private final UsernameService usernames;
    private final Clock reloj;

    @Autowired
    public CambioDeNombreDeUsuario(UserRepository userRepository, UsernameService usernames) {
        this(userRepository, usernames, Clock.systemUTC());
    }

    /** Con otro reloj, para los tests. */
    CambioDeNombreDeUsuario(UserRepository userRepository, UsernameService usernames, Clock reloj) {
        this.userRepository = userRepository;
        this.usernames = usernames;
        this.reloj = reloj;
    }

    /**
     * El primer día en que puede cambiarlo otra vez, o null si ya puede.
     *
     * Se cuenta en días de Buenos Aires: si lo cambió el 1 a las 23:30, puede de nuevo
     * desde las 00:00 del 31. El período es [desde, hasta), así que ese día ya vale.
     */
    public LocalDate proximoCambio(Instant cambiadoEl) {
        if (cambiadoEl == null) {
            return null;
        }
        LocalDate desde = cambiadoEl.atZone(Logros.ZONA).toLocalDate().plusDays(DIAS_ENTRE_CAMBIOS);
        LocalDate hoy = reloj.instant().atZone(Logros.ZONA).toLocalDate();
        return hoy.isBefore(desde) ? desde : null;
    }

    /** Para la pantalla de ajustes. */
    @Transactional(readOnly = true)
    public com.hamburguesas.dto.EstadoDelNombreDto estado(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("No encontramos tu cuenta"));
        LocalDate desde = proximoCambio(user.getUsernameChangedAt());
        return new com.hamburguesas.dto.EstadoDelNombreDto(desde, desde == null ? null : enPalabras(desde));
    }

    @Transactional
    public AuthResponse cambiar(Long userId, String crudo) {
        // El mismo lock que usan otros cambios de la cuenta: dos pedidos de la misma
        // persona se hacen en fila y el segundo ve el cambio del primero.
        User user = userRepository.bloquear(userId)
            .orElseThrow(() -> new ResourceNotFoundException("No encontramos tu cuenta"));

        String nombre = com.hamburguesas.auth.Usernames.normalizar(crudo);
        // Pedir el que ya tiene no es un cambio: no gasta los 30 días.
        if (nombre.equals(user.getUsername())) {
            return SessionIssuer.quienEs(user);
        }

        // Primero el plazo: quien no puede cambiarlo no tiene por qué enterarse de qué
        // nombres están tomados.
        LocalDate desde = proximoCambio(user.getUsernameChangedAt());
        if (desde != null) {
            throw new UsernameCooldownException(
                "Podés volver a cambiar tu nombre de usuario el " + enPalabras(desde), desde);
        }

        user.setUsername(usernames.reservar(nombre));
        user.setUsernameChangedAt(reloj.instant());
        try {
            // Se escribe acá y no al cerrar la transacción: si otra persona se quedó con
            // el nombre en el medio, el unique de la base lo dice ahora y se contesta
            // "en uso", no un error 500.
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new UsernameTakenException("Ese nombre de usuario ya está en uso");
        }
        return SessionIssuer.quienEs(user);
    }

    /** "12 de diciembre". */
    static String enPalabras(LocalDate dia) {
        return dia.getDayOfMonth() + " de "
            + dia.getMonth().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es-AR"));
    }
}
