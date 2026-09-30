package com.hamburguesas.places;

import com.hamburguesas.model.PlacesCallType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Manual trigger for the Places sync, guarded by a shared-secret header instead of user auth
 * since this is meant to be called by us (or a cron caller), not by app users.
 */
@RestController
@RequestMapping("/api/admin/places-sync")
@RequiredArgsConstructor
public class PlacesSyncController {

    private static final String TOKEN_HEADER = "X-Sync-Token";

    private final PlacesSyncService syncService;
    private final PlacesQuotaGuard quotaGuard;
    private final PlacesProperties properties;

    /** Behind the token as well: our quota and configuration are nobody else's business. */
    @GetMapping("/status")
    public ResponseEntity<PlacesSyncStatus> status(
        @RequestHeader(name = TOKEN_HEADER, required = false) String token
    ) {
        if (!authorized(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(new PlacesSyncStatus(
            properties.hasApiKey(),
            quotaGuard.used(PlacesCallType.SEARCH),
            quotaGuard.limitFor(PlacesCallType.SEARCH),
            quotaGuard.used(PlacesCallType.PHOTO),
            quotaGuard.limitFor(PlacesCallType.PHOTO)
        ));
    }

    @PostMapping
    public ResponseEntity<?> trigger(@RequestHeader(name = TOKEN_HEADER, required = false) String token) {
        if (!authorized(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(syncService.sync());
    }

    /**
     * Solo las fotos: completa las que faltan y vuelve a elegir las viejas.
     *
     * Aparte del disparador completo porque son dos trabajos de costo muy distinto. La
     * sincronización entera gasta hasta mil búsquedas recorriendo los 48 barrios; esto
     * no busca nada, y es lo único que hace falta cuando lo que cambió es la regla de
     * elección de foto.
     */
    @PostMapping("/fotos")
    public ResponseEntity<?> revisarFotos(
        @RequestHeader(name = TOKEN_HEADER, required = false) String token
    ) {
        if (!authorized(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(syncService.revisarFotos());
    }

    private boolean authorized(String presented) {
        String expected = properties.getSync().getTriggerToken();
        if (expected == null || expected.isBlank() || presented == null) {
            return false;
        }

        // Constant time. String.equals stops at the first byte that differs, so how
        // long it takes leaks how much of the token was right, and a token can be
        // rebuilt one character at a time from that.
        return MessageDigest.isEqual(
            expected.getBytes(StandardCharsets.UTF_8),
            presented.getBytes(StandardCharsets.UTF_8));
    }

    public record PlacesSyncStatus(
        boolean apiKeyConfigured,
        int searchCallsUsed,
        int searchCallsLimit,
        int photoCallsUsed,
        int photoCallsLimit
    ) {}
}
