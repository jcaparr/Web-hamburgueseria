package com.hamburguesas.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Optional;

/**
 * Checks the ID token the browser gets from Google.
 *
 * The token is the whole basis for trusting who the user is, so every check matters:
 * the signature has to be Google's, the audience has to be *our* client id (a token
 * minted for another site would otherwise log its users into ours), and the address
 * has to be one Google itself has verified.
 */
@Component
@Slf4j
public class GoogleTokenVerifier {

    /** What we trust from a verified token. */
    public record GoogleAccount(String subject, String email, String name) {}

    private final GoogleIdTokenVerifier verifier;
    private final boolean configured;

    public GoogleTokenVerifier(AuthProperties properties) {
        String clientId = properties.getGoogle().getClientId();
        this.configured = clientId != null && !clientId.isBlank();

        this.verifier = configured
            ? new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(clientId))
                .build()
            : null;
    }

    public boolean isConfigured() {
        return configured;
    }

    /** @return empty when the token is not valid, whatever the reason. */
    public Optional<GoogleAccount> verify(String idTokenString) {
        if (!configured) {
            return Optional.empty();
        }

        GoogleIdToken idToken;
        try {
            // Returns null (rather than throwing) when the token simply does not check out.
            idToken = verifier.verify(idTokenString);
        } catch (GeneralSecurityException | IOException ex) {
            log.warn("Could not verify a Google token: {}", ex.getMessage());
            return Optional.empty();
        }

        if (idToken == null) {
            return Optional.empty();
        }

        GoogleIdToken.Payload payload = idToken.getPayload();

        // Google will hand out a token for an address the account has not confirmed.
        // Trusting it would let someone claim an address that is not theirs.
        if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
            log.warn("Rejected a Google token whose email is not verified");
            return Optional.empty();
        }

        String email = payload.getEmail();
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }

        Object name = payload.get("name");
        return Optional.of(new GoogleAccount(
            payload.getSubject(),
            email.trim().toLowerCase(),
            name != null ? name.toString() : email));
    }
}
