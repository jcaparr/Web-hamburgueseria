package com.hamburguesas.podio;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;

/**
 * Los cierres del podio (#318), en hora de Buenos Aires: el lunes a las 00:05 cierra la
 * semana que terminó y el día 1 a las 00:05, el mes. Cada cierre guarda su resultado y da
 * los logros de ranking; repetirlo no hace nada.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CierreDePodio {

    private final Podio podio;

    /**
     * Al arrancar cierra la última semana y el último mes si todavía no están cerrados: si el
     * servidor estaba caído o se actualizaba a las 00:05, ese cierre no se pierde (y un mes
     * perdido no corta la Racha de oro). Cerrar es idempotente.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void recuperarCierresPerdidos() {
        try {
            cerrarLaSemana();
            cerrarElMes();
        } catch (RuntimeException ex) {
            log.warn("Podio: no se pudieron recuperar los cierres pendientes", ex);
        }
    }

    @Scheduled(cron = "${app.podio.cron-semana:0 5 0 * * MON}", zone = "America/Argentina/Buenos_Aires")
    public void cerrarLaSemana() {
        LocalDate hoy = LocalDate.now(Podio.ZONA);
        LocalDate lunesAnterior = hoy.with(TemporalAdjusters.previous(DayOfWeek.MONDAY));
        podio.cerrarSemana(lunesAnterior);
        log.info("Podio: cerrada la semana del {}", lunesAnterior);
    }

    @Scheduled(cron = "${app.podio.cron-mes:0 5 0 1 * *}", zone = "America/Argentina/Buenos_Aires")
    public void cerrarElMes() {
        YearMonth anterior = YearMonth.now(Podio.ZONA).minusMonths(1);
        podio.cerrarMes(anterior);
        log.info("Podio: cerrado el mes {}", anterior);
    }
}
