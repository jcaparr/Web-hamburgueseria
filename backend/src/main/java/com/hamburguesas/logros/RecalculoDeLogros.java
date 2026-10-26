package com.hamburguesas.logros;

import com.hamburguesas.model.User;
import com.hamburguesas.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Cuándo se recalculan los logros (#314): al confirmar cada cambio, de noche para todos, y
 * una vez al lanzar, para darles a las cuentas que ya existen lo que ya merecían.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecalculoDeLogros implements ApplicationRunner {

    private final Logros logros;
    private final LogroDeUsuarioRepository repository;
    private final UserRepository userRepository;

    /**
     * Corre después de confirmar y en una transacción propia: la del cambio ya terminó.
     * Si falla se avisa en el log y lo hecho queda; la tarea de la noche lo corrige.
     * Con {@code fallbackExecution} también corre cuando quien publica no tiene transacción.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void alCambiar(LogrosPorRecalcular evento) {
        for (Long cuenta : evento.cuentas()) {
            try {
                logros.recalcular(cuenta, false);
            } catch (RuntimeException ex) {
                log.warn("No se pudieron recalcular los logros de la cuenta {}", cuenta, ex);
            }
        }
    }

    /**
     * La tarea de la noche: recalcula a todos, por si algún cambio no avisó (por ejemplo
     * quien reseñó un local después que vos, para Boca en boca). A las 3:15, hora de Buenos
     * Aires: el barrido de Places es los domingos a las 4 y por defecto está apagado, así que
     * no se pisan.
     */
    @Scheduled(cron = "${app.logros.cron:0 15 3 * * *}", zone = "America/Argentina/Buenos_Aires")
    public void recalcularTodas() {
        log.info("Logros: recalculadas {} cuentas", recalcularTodas(false));
    }

    /**
     * A todas las cuentas, una por vez y cada una en su transacción: una con un problema no
     * frena a las demás. Va acá y no en {@link Logros} para pasar por su proxy transaccional.
     */
    int recalcularTodas(boolean retroactivo) {
        int hechas = 0;
        for (Long id : userRepository.findAll().stream().map(User::getId).toList()) {
            try {
                logros.recalcular(id, retroactivo);
                hechas++;
            } catch (RuntimeException ex) {
                log.warn("No se pudieron recalcular los logros de la cuenta {}", id, ex);
            }
        }
        return hechas;
    }

    /**
     * La carga inicial: si todavía no hay ningún logro y sí hay cuentas, recibe cada una lo
     * que ya merecía, marcado como retroactivo para avisarlo de a uno solo ("Ya tenías N logros").
     */
    @Override
    public void run(ApplicationArguments args) {
        if (repository.count() == 0) {
            int hechas = recalcularTodas(true);
            if (hechas > 0) {
                log.info("Logros: carga inicial de {} cuentas", hechas);
            }
        }
    }
}
