package com.hamburguesas.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

/**
 * Asks Have I Been Pwned whether a password has appeared in a known breach.
 *
 * The password never leaves this server. Its SHA-1 is computed here and only the
 * first five characters are sent; the answer is every hash in the service that
 * starts with those five, and the comparison happens locally. HIBP cannot tell which
 * of the hundreds it returned we were asking about, nor whose account it was for.
 *
 * The check is about reuse, not strength: "correcthorsebatterystaple" is a fine
 * password until it shows up in a dump, and then it is the first thing anyone tries.
 */
@Component
@Slf4j
public class PwnedPasswordChecker {

    private static final String RANGE_URL = "https://api.pwnedpasswords.com/range/";

    private final RestClient restClient;
    private final AuthProperties properties;

    public PwnedPasswordChecker(AuthProperties properties) {
        this.properties = properties;

        // Short timeouts: this sits in the middle of registering, and a slow third
        // party must not turn into a slow sign-up.
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(3));

        this.restClient = RestClient.builder()
            .requestFactory(factory)
            .baseUrl(RANGE_URL)
            .defaultHeader("Add-Padding", "true")
            .build();
    }

    /**
     * Prueba al arrancar que el chequeo realmente funciona.
     *
     * Dejar pasar la contraseña cuando HIBP no responde es aceptable para una caída
     * pasajera: se vuelve al estado anterior a tener el chequeo. Lo que no es
     * aceptable es que falle siempre —una regla de firewall, un proxy, la salida
     * HTTPS cerrada— y que eso se note solo en un warning que nadie lee. Quedaría una
     * protección apagada en la que igual se confía.
     *
     * Por eso al arrancar se consulta una contraseña que con certeza está filtrada.
     * Si la respuesta no es la esperada, el problema queda en un ERROR al inicio del
     * log, donde se mira, y no escondido entre los registros de cada usuario.
     *
     * Va en ApplicationReadyEvent y no en PostConstruct para no demorar el arranque
     * ni tumbarlo si el servicio está caído: es diagnóstico, no un requisito.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void verifyItWorks() {
        if (!properties.getPassword().isCheckBreaches()) {
            log.warn("El chequeo de contraseñas filtradas está apagado");
            return;
        }

        if (isBreached("password")) {
            log.info("Chequeo de contraseñas filtradas: funcionando");
        } else {
            log.error("El chequeo de contraseñas filtradas NO está funcionando: HIBP no "
                + "respondió lo esperado para una contraseña que sí está filtrada. "
                + "Se van a aceptar contraseñas conocidas hasta que se resuelva.");
        }
    }

    /**
     * @return true when the password is known to be breached. Returns false if HIBP
     *         cannot be reached: refusing every registration because somebody else's
     *         service is down would be worse than letting a weak password through.
     *         El arranque avisa si esa situación es permanente y no pasajera.
     */
    public boolean isBreached(String password) {
        if (!properties.getPassword().isCheckBreaches()) {
            return false;
        }

        String hash = sha1(password);
        String prefix = hash.substring(0, 5);
        String suffix = hash.substring(5);

        try {
            String body = restClient.get().uri(prefix).retrieve().body(String.class);
            if (body == null) {
                return false;
            }

            return body.lines().anyMatch(line -> {
                int separator = line.indexOf(':');
                String candidate = separator > 0 ? line.substring(0, separator) : line;
                // Padding responses carry a count of zero: those are filler, not hits.
                String count = separator > 0 ? line.substring(separator + 1).trim() : "0";
                return candidate.equalsIgnoreCase(suffix) && !"0".equals(count);
            });
        } catch (RuntimeException ex) {
            log.warn("No se pudo consultar HIBP, se deja pasar la contraseña: {}", ex.getMessage());
            return false;
        }
    }

    private String sha1(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            return HexFormat.of().withUpperCase()
                .formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-1 no disponible", ex);
        }
    }
}
