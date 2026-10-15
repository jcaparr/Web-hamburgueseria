package com.hamburguesas.service;

import com.hamburguesas.dto.SugerenciaDeLocalRequest;
import com.hamburguesas.exception.EmailNotVerifiedException;
import com.hamburguesas.exception.ResourceNotFoundException;
import com.hamburguesas.exception.TooManyRequestsException;
import com.hamburguesas.mail.EmailService;
import com.hamburguesas.model.SugerenciaDeLocal;
import com.hamburguesas.model.User;
import com.hamburguesas.repository.SugerenciaDeLocalRepository;
import com.hamburguesas.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.Instant;

/**
 * Guarda los locales que la gente avisa que faltan (#257) y le manda un mail a Juan con
 * cada uno.
 *
 * No llama a Google de ninguna forma: es texto que Juan revisa a mano. Para que no lo
 * revienten con un bot ni con muchas cuentas pide sesión con el email verificado, un
 * minuto entre una y otra, un tope por persona y otro entre todos. Los mails tienen su
 * propio tope, más bajo, para no comerse los 300 diarios de Brevo.
 *
 * Los topes cuentan y después guardan, sin bloquear: dos pedidos exactamente a la vez
 * podrían pasarse por uno. Para frenar abusos alcanza.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SugerenciasDeLocales {

    private static final Duration UN_DIA = Duration.ofDays(1);

    private final SugerenciaDeLocalRepository sugerencias;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final SugerenciasProperties topes;

    @Transactional
    public void sugerir(Long userId, SugerenciaDeLocalRequest pedido) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException("Para sugerir un local, primero verificá tu email.");
        }

        Instant ahora = Instant.now();
        if (sugerencias.countByUser_IdAndCreatedAtAfter(userId, ahora.minus(topes.getMinimoEntreUnaYOtra())) > 0) {
            throw new TooManyRequestsException("Esperá un minuto antes de mandar otra.");
        }
        if (sugerencias.countByUser_IdAndCreatedAtAfter(userId, ahora.minus(UN_DIA)) >= topes.getTopePorPersona()) {
            throw new TooManyRequestsException("Ya mandaste varias hoy. Probá mañana.");
        }
        long deHoy = sugerencias.countByCreatedAtAfter(ahora.minus(UN_DIA));
        if (deHoy >= topes.getTopeTotal()) {
            throw new TooManyRequestsException("Hoy recibimos muchas sugerencias. Probá mañana.");
        }

        SugerenciaDeLocal sugerencia = sugerencias.save(SugerenciaDeLocal.builder()
            .user(user)
            .nombre(pedido.nombre().strip())
            .direccion(pedido.direccion().strip())
            .email(pedido.email().strip())
            .comentario(vacioANulo(pedido.comentario()))
            .createdAt(ahora)
            .build());

        // Pasado el tope de mails se guarda igual, sin aviso: Juan la ve en la base.
        if (deHoy >= topes.getTopeDeMails()) {
            log.warn("Sugerencia {} guardada sin mail: ya van {} hoy", sugerencia.getId(), deHoy + 1);
            return;
        }
        // Después de confirmar: si la base no la guardó, que no llegue un aviso de algo
        // que no está.
        String usuario = user.getUsername();
        despuesDeConfirmar(() -> avisar(sugerencia, usuario));
    }

    /**
     * El mail a Juan. Si falla, la sugerencia ya está guardada y se puede ver en la base:
     * a quien la mandó no se le dice que salió mal algo que no le toca arreglar.
     */
    private void avisar(SugerenciaDeLocal sugerencia, String usuario) {
        // En una sola línea: un salto de línea en el asunto rompe el encabezado del mail.
        String asunto = "Sugerencia de local: " + sugerencia.getNombre().replaceAll("\\s+", " ");
        String cuerpo = """
            Alguien sugirió un local para Burgómetro.

            Nombre: %s
            Dirección o link: %s
            Email de contacto: %s
            Comentario: %s
            Usuario: @%s

            Respondiendo este mail le escribís a quien lo sugirió.
            """.formatted(
                sugerencia.getNombre(),
                sugerencia.getDireccion(),
                sugerencia.getEmail(),
                sugerencia.getComentario() == null ? "(sin comentario)" : sugerencia.getComentario(),
                usuario);
        try {
            emailService.send(topes.getMailA(), asunto, cuerpo, sugerencia.getEmail());
        } catch (RuntimeException ex) {
            log.error("No se pudo mandar el aviso de la sugerencia {}: {}", sugerencia.getId(), ex.getMessage());
        }
    }

    /** Fuera de una transacción (en un test, por ejemplo) no hay qué esperar: corre ya. */
    private static void despuesDeConfirmar(Runnable accion) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    accion.run();
                }
            });
        } else {
            accion.run();
        }
    }

    private static String vacioANulo(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.strip();
    }
}
