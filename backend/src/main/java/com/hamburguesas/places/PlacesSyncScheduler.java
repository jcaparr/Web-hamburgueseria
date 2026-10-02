package com.hamburguesas.places;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class PlacesSyncScheduler {

    private final PlacesSyncService syncService;
    private final PlacesProperties properties;

    /** Cron is configurable (app.places.sync.cron); defaults to Sundays 4am. Off unless app.places.sync.enabled=true. */
    @Scheduled(cron = "${app.places.sync.cron:0 0 4 * * SUN}")
    public void run() {
        if (!properties.getSync().isEnabled()) {
            return;
        }
        log.info("Starting scheduled Places sync");
        PlacesSyncReport report = syncService.sync();
        log.info("Scheduled Places sync report: {}", report);

        // Después del barrido, para que los locales que acaba de traer ya estén. Sale de
        // otra cuota, así que no le quita nada: el primer domingo del mes gasta la del
        // mes y los siguientes solo preguntan por lo que haya quedado.
        HorariosPedidos horarios = syncService.completarHorarios();
        log.info("Scheduled horarios report: {}", horarios);
    }
}
