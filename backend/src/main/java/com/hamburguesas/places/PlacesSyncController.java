package com.hamburguesas.places;

import com.hamburguesas.model.PlacesCallType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/status")
    public PlacesSyncStatus status() {
        return new PlacesSyncStatus(
            properties.hasApiKey(),
            quotaGuard.used(PlacesCallType.SEARCH),
            quotaGuard.limitFor(PlacesCallType.SEARCH),
            quotaGuard.used(PlacesCallType.PHOTO),
            quotaGuard.limitFor(PlacesCallType.PHOTO)
        );
    }

    @PostMapping
    public ResponseEntity<?> trigger(@RequestHeader(name = TOKEN_HEADER, required = false) String token) {
        String expected = properties.getSync().getTriggerToken();
        if (expected == null || expected.isBlank() || !expected.equals(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(syncService.sync());
    }

    public record PlacesSyncStatus(
        boolean apiKeyConfigured,
        int searchCallsUsed,
        int searchCallsLimit,
        int photoCallsUsed,
        int photoCallsLimit
    ) {}
}
