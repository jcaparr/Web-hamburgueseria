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
    }
}
