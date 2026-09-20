package com.hamburguesas.auth;

import lombok.extern.slf4j.Slf4j;
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
     * @return true when the password is known to be breached. Returns false if HIBP
     *         cannot be reached: refusing every registration because somebody else's
     *         service is down would be worse than letting a weak password through.
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
