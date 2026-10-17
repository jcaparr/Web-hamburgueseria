package com.hamburguesas.service;

import com.hamburguesas.dto.SugerenciaDeLocalRequest;
import com.hamburguesas.exception.ConflictException;
import com.hamburguesas.exception.EmailNotVerifiedException;
import com.hamburguesas.exception.ResourceNotFoundException;
import com.hamburguesas.exception.TooManyRequestsException;
import com.hamburguesas.fotos.FotosDeSugerencias;
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
 * La foto es opcional (#283). Se procesa igual que las de las reseñas pero se guarda
 * aparte y nada la sirve: la ve Juan, que la aprueba al agregar el local. Cuenta para los
 * mismos topes que el resto de la sugerencia, más un tope de peso por foto.
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
    private final FotosDeSugerencias fotos;

    public void sugerir(Long userId, SugerenciaDeLocalRequest pedido) {
        sugerir(userId, pedido, null);
    }

    /** @param foto los bytes de la foto opcional, o nulo si no mandó ninguna. */
    @Transactional
    public void sugerir(Long userId, SugerenciaDeLocalRequest pedido, byte[] foto) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException("Para sugerir un local, primero verificá tu email.");
        }

        // El del día antes que el del minuto: a quien ya llegó a 5, decirle que espere un
        // minuto lo hacía volver para recién ahí enterarse de que no podía hasta mañana.
        Instant ahora = Instant.now();
        if (sugerencias.countByUser_IdAndCreatedAtAfter(userId, ahora.minus(UN_DIA)) >= topes.getTopePorPersona()) {
            throw new TooManyRequestsException("Ya mandaste varias hoy. Probá mañana.");
        }
        if (sugerencias.countByUser_IdAndCreatedAtAfter(userId, ahora.minus(topes.getMinimoEntreUnaYOtra())) > 0) {
            throw new TooManyRequestsException("Esperá un minuto antes de mandar otra.");
        }
        long deHoy = sugerencias.countByCreatedAtAfter(ahora.minus(UN_DIA));
        if (deHoy >= topes.getTopeTotal()) {
            throw new TooManyRequestsException("Hoy recibimos muchas sugerencias. Probá mañana.");
        }

        if (foto != null && foto.length > topes.getPesoMaximoDeFoto().toBytes()) {
            throw new ConflictException("La foto pesa demasiado. El máximo es "
                + topes.getPesoMaximoDeFoto().toMegabytes() + " MB.");
        }
        // Después de los topes y antes de guardar: una foto no se procesa para un pedido
        // que se iba a rechazar igual, y si la foto no sirve no queda la sugerencia a medias.
        String archivoDeFoto = foto == null || foto.length == 0 ? null : fotos.guardar(foto);

        SugerenciaDeLocal sugerencia;
        try {
            sugerencia = sugerencias.save(SugerenciaDeLocal.builder()
                .user(user)
                .nombre(pedido.nombre().strip())
                .direccion(pedido.direccion().strip())
                .email(pedido.email().strip())
                .comentario(vacioANulo(pedido.comentario()))
                .fotoArchivo(archivoDeFoto)
                .createdAt(ahora)
                .build());
        } catch (RuntimeException ex) {
            // Que no quede en disco una foto que ninguna sugerencia apunta.
            if (archivoDeFoto != null) {
                fotos.borrar(archivoDeFoto);
            }
            throw ex;
        }

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
            Foto: %s
            Usuario: @%s

            Respondiendo este mail le escribís a quien lo sugirió.
            """.formatted(
                sugerencia.getNombre(),
                sugerencia.getDireccion(),
                sugerencia.getEmail(),
                sugerencia.getComentario() == null ? "(sin comentario)" : sugerencia.getComentario(),
                // Sin el archivo adjunto: la foto se mira en el servidor, no sale por mail.
                sugerencia.getFotoArchivo() == null
                    ? "(sin foto)" : "SÍ, trae foto (sugerencias-fotos/" + sugerencia.getFotoArchivo() + ")",
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
