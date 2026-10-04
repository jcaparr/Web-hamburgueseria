package com.hamburguesas.places;

import com.hamburguesas.model.PlacesCallType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

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
    public ResponseEntity<?> status(
        @RequestHeader(name = TOKEN_HEADER, required = false) String token
    ) {
        return siEstaAutorizado(token, () -> new PlacesSyncStatus(
            properties.hasApiKey(),
            quotaGuard.mesEnCurso(),
            Arrays.stream(PlacesCallType.values())
                .map(tipo -> UsoDeCuota.de(tipo, quotaGuard.used(tipo), quotaGuard.limitFor(tipo)))
                .toList()
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
        return siEstaAutorizado(token, () -> syncService.sync(conFotos));
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
        return siEstaAutorizado(token, syncService::revisarFotos);
    }

    /**
     * Solo pregunta: de qué locales Google no tiene ninguna foto.
     *
     * Aparte del de fotos porque son dos cuotas muy desparejas —cuatro mil fichas contra
     * mil fotos— y conviene poder gastar la barata sola. Preguntar por los mil doscientos
     * locales sin portada entra holgado en las fichas del mes y dice cuántos quedan en
     * pie una vez que la limpieza borre los que Google no tiene fotografiados.
     */
    @PostMapping("/fichas")
    public ResponseEntity<?> revisarFichas(
        @RequestHeader(name = TOKEN_HEADER, required = false) String token
    ) {
        return siEstaAutorizado(token, syncService::revisarFichas);
    }

    /**
     * Solo el préstamo de portadas entre sucursales de la misma marca.
     *
     * Aparte de la revisión de fotos porque no le pide nada a Google: la foto ya está
     * bajada y se le apunta la misma a la hermana que no tiene. La revisión, en cambio,
     * gasta una ficha por local y una foto por cada uno que cambie.
     *
     * No pisa ninguna portada: solo mira los locales que no tienen.
     */
    @PostMapping("/fotos-prestadas")
    public ResponseEntity<?> prestarFotos(
        @RequestHeader(name = TOKEN_HEADER, required = false) String token
    ) {
        return siEstaAutorizado(token, syncService::prestarFotos);
    }

    /**
     * Solo la limpieza: borra lo que ya no corresponde y recalcula las zonas.
     *
     * Aparte del barrido porque esta mitad no cuesta ninguna búsqueda y la otra cuesta
     * mil novecientas. Cuando lo que hace falta es aplicar lo que un censo de fichas ya
     * averiguó, correr el barrido entero es pagar la parte cara para ejecutar la gratis.
     */
    @PostMapping("/limpieza")
    public ResponseEntity<?> limpiar(
        @RequestHeader(name = TOKEN_HEADER, required = false) String token
    ) {
        return siEstaAutorizado(token, syncService::limpiar);
    }

    /**
     * Solo los horarios: se los pide a Google a los locales que no tienen, y a los que
     * lo tienen hace más de un mes.
     *
     * Sale de su propia cuota, de 950 por mes. Son más locales que eso, así que lo que
     * no entra lo completa la pasada del mes siguiente.
     */
    @PostMapping("/horarios")
    public ResponseEntity<?> completarHorarios(
        @RequestHeader(name = TOKEN_HEADER, required = false) String token
    ) {
        return siEstaAutorizado(token, syncService::completarHorarios);
    }

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
        return siEstaAutorizado(token, () -> syncService.agregar(texto));
    }

    /**
     * Corre el trabajo solo si el token es el que corresponde.
     *
     * El trabajo se pasa sin correr y se ejecuta recién después de mirar el token: con un
     * token equivocado no se gasta ni una llamada de la cuota.
     */
    private ResponseEntity<?> siEstaAutorizado(String token, Supplier<?> trabajo) {
        if (!authorized(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(trabajo.get());
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

    /**
     * @param cuotas una por cada tipo de llamada que se cuenta, en el orden del enum.
     *               Antes eran campos sueltos para búsquedas y fotos, y cada tipo nuevo
     *               —fichas, resúmenes, horarios— quedaba afuera hasta que alguien se
     *               acordara de sumarlo: en octubre la de fichas llegó al tope y acá no
     *               se veía. Recorriendo el enum, el próximo tipo aparece solo.
     */
    public record PlacesSyncStatus(boolean apiKeyConfigured, String mes, List<UsoDeCuota> cuotas) {}

    /** @param quedan las que todavía se pueden hacer este mes; nunca negativo. */
    public record UsoDeCuota(PlacesCallType tipo, int usadas, int tope, int quedan) {

        static UsoDeCuota de(PlacesCallType tipo, int usadas, int tope) {
            return new UsoDeCuota(tipo, usadas, tope, Math.max(0, tope - usadas));
        }
    }
}
