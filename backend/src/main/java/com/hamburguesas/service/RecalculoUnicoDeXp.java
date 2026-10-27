package com.hamburguesas.service;

import com.hamburguesas.logros.LogrosPorRecalcular;
import com.hamburguesas.model.TareaUnica;
import com.hamburguesas.model.User;
import com.hamburguesas.repository.RatingRepository;
import com.hamburguesas.repository.TareaUnicaRepository;
import com.hamburguesas.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Recalcula una vez la XP de todas las cuentas con la regla de 5 palabras (#344).
 *
 * La XP de cada cuenta se guarda y solo se recalcula cuando cambia una de sus reseñas, así
 * que sin esto las cuentas con comentarios de entre 5 y 9 palabras seguirían con la XP de la
 * regla vieja, mientras Logros y el Podio por período ya usan la nueva. Después de esto,
 * nivel, Logros y Podio coinciden.
 *
 * Usa {@link RecalculoDeXp}, la misma cuenta de siempre: la regla no se repite acá.
 *
 * <ul>
 *   <li>Solo sube: si la XP que da la regla es menor que la guardada, la cuenta no se toca
 *   y se avisa en el log. La regla nueva es más laxa, así que no debería pasar.</li>
 *   <li>Idempotente: correrla de nuevo no cambia nada. Igual se anota en
 *   {@code tareas_unicas} para no recorrer todas las cuentas en cada arranque.</li>
 *   <li>Si una cuenta falla se sigue con las demás y no se anota como hecha: el próximo
 *   arranque retoma las que faltaron.</li>
 *   <li>A las cuentas que suben se les piden los logros de nivel, como al publicar una
 *   reseña.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecalculoUnicoDeXp implements ApplicationRunner {

    static final String NOMBRE = "xp-con-5-palabras";

    private final UserRepository userRepository;
    private final RatingRepository ratingRepository;
    private final RecalculoDeXp recalculoDeXp;
    private final TareaUnicaRepository tareas;
    private final ApplicationEventPublisher eventos;

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (tareas.existsById(NOMBRE)) {
                return;
            }
            Resultado resultado = ejecutar();
            log.info("XP con la regla de 5 palabras: {} cuentas revisadas, {} subieron, {} sin tocar por quedar más abajo, {} con error",
                resultado.revisadas(), resultado.subieron(), resultado.sinTocar(), resultado.conError());
            if (resultado.conError() == 0) {
                tareas.save(new TareaUnica(NOMBRE, Instant.now()));
            }
        } catch (RuntimeException ex) {
            log.warn("No se pudo recalcular la XP con la regla de 5 palabras; se reintenta al próximo arranque", ex);
        }
    }

    /** El recálculo en sí, sin la marca de "ya hecho". */
    Resultado ejecutar() {
        int revisadas = 0;
        int subieron = 0;
        int sinTocar = 0;
        int conError = 0;
        for (User cuenta : userRepository.findAll()) {
            revisadas++;
            try {
                int guardada = cuenta.getXp();
                int nueva = Niveles.xpTotal(ratingRepository.comentariosDe(cuenta.getId()));
                if (nueva < guardada) {
                    sinTocar++;
                    log.warn("La cuenta {} quedaría con menos XP ({} a {}): no se toca", cuenta.getId(), guardada, nueva);
                } else if (nueva > guardada) {
                    recalculoDeXp.recalcular(cuenta.getId());
                    eventos.publishEvent(LogrosPorRecalcular.de(cuenta.getId()));
                    subieron++;
                }
            } catch (RuntimeException ex) {
                conError++;
                log.warn("No se pudo recalcular la XP de la cuenta {}", cuenta.getId(), ex);
            }
        }
        return new Resultado(revisadas, subieron, sinTocar, conError);
    }

    record Resultado(int revisadas, int subieron, int sinTocar, int conError) {
    }
}
