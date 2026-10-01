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

    /**
     * @param conFotos false para traer locales sin bajar ni una foto. Las dos cuotas son
     *                 muy desparejas —cuatro mil búsquedas por mes contra mil fotos— y
     *                 recorrer las 89 zonas sale unas mil doscientas búsquedas, mientras
     *                 que ponerle portada a todo lo que entra no alcanza ni de cerca.
     */
    @PostMapping
    public ResponseEntity<?> trigger(
        @RequestHeader(name = TOKEN_HEADER, required = false) String token,
        @RequestParam(name = "conFotos", defaultValue = "true") boolean conFotos
    ) {
        if (!authorized(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(syncService.sync(conFotos));
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

    /**
     * Solo pregunta: de qué locales Google no tiene ninguna foto.
     *
     * Aparte del de fotos porque son dos cuotas muy desparejas —cuatro mil fichas contra
     * mil fotos— y conviene poder gastar la barata sola. Preguntar por los mil doscientos
     * locales sin portada entra holgado en las fichas del mes y dice cuántos quedan en
     * pie una vez que la limpieza borre los que Google no tiene fotografiados.
     */
    /**
     * Solo la limpieza: borra lo que ya no corresponde y recalcula las zonas.
     *
     * Aparte del barrido porque esta mitad no cuesta ninguna búsqueda y la otra cuesta
     * mil novecientas. Cuando lo que hace falta es aplicar lo que un censo de fichas ya
     * averiguó, correr el barrido entero es pagar la parte cara para ejecutar la gratis.
     */
    /**
     * Agrega un local puntual buscándolo por su nombre.
     *
     * Para las hamburgueserías que el barrido no encuentra nunca: Google no les pone el
     * rubro, así que la búsqueda estricta por barrio no las devuelve por más veces que
     * se corra. Devuelve el identificador, que es lo que después hay que anotar en
     * included-place-ids para que la limpieza no lo borre.
     */
    @PostMapping("/agregar")
    public ResponseEntity<?> agregar(
        @RequestHeader(name = TOKEN_HEADER, required = false) String token,
        @RequestParam("texto") String texto
    ) {
        if (!authorized(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(syncService.agregar(texto));
    }

    @PostMapping("/limpieza")
    public ResponseEntity<?> limpiar(
        @RequestHeader(name = TOKEN_HEADER, required = false) String token
    ) {
        if (!authorized(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(syncService.limpiar());
    }

    @PostMapping("/fichas")
    public ResponseEntity<?> revisarFichas(
        @RequestHeader(name = TOKEN_HEADER, required = false) String token
    ) {
        if (!authorized(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(syncService.revisarFichas());
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
