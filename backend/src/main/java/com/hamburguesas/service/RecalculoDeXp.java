package com.hamburguesas.service;

import com.hamburguesas.model.User;
import com.hamburguesas.repository.RatingRepository;
import com.hamburguesas.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Deja en {@code users.xp} lo que suman hoy las reseñas de la persona (#310).
 *
 * Se recalcula todo desde las reseñas y no se suma o resta lo que cambió: así el número
 * es siempre el mismo si se repite, y borrar, editar o un evento perdido no lo desvían.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecalculoDeXp {

    private final RatingRepository ratingRepository;
    private final UserRepository userRepository;

    /**
     * Corre después de confirmar y en una transacción propia: la de la reseña ya terminó
     * y no se puede escribir en ella. Si falla se avisa en el log y la reseña queda; la
     * próxima reseña, o el próximo borrado, vuelve a dejar el número bien.
     *
     * Con {@code fallbackExecution} también corre cuando quien publica no tiene transacción.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void alCambiarLasResenias(XpPorRecalcular evento) {
        try {
            recalcular(evento.userId());
        } catch (RuntimeException ex) {
            log.warn("No se pudo recalcular la XP de la cuenta {}", evento.userId(), ex);
        }
    }

    /**
     * Si bajó de nivel, la hamburguesa pierde lo que ya no alcanza (#313): huevo, bacon,
     * cuarta carne y rayos. Se escribe con compare-and-set: si en el medio la cambió ella,
     * el recorte no pisa lo nuevo.
     */
    private void recortarLosPremios(User cuenta, int xp) {
        if (cuenta == null || cuenta.getHamburguesa() == null) {
            return;
        }
        String vieja = cuenta.getHamburguesa();
        String nueva = Premios.recortar(vieja, Niveles.nivelDe(xp));
        if (!nueva.equals(vieja)) {
            userRepository.recortarHamburguesa(cuenta.getId(), vieja, nueva);
        }
    }

    /** Devuelve la XP que quedó. */
    @Transactional
    public int recalcular(Long userId) {
        // La fila de la cuenta se bloquea: dos recálculos casi juntos se esperan en vez de pisarse
        // con un número viejo.
        User cuenta = userRepository.bloquear(userId).orElse(null);
        int xp = Niveles.xpTotal(ratingRepository.comentariosDe(userId));
        userRepository.ponerXp(userId, xp);
        recortarLosPremios(cuenta, xp);
        return xp;
    }
}
