package com.hamburguesas.auth;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {

    private Verification verification = new Verification();

    @Data
    public static class Verification {
        /** Short enough to type from a phone, long enough that 5 guesses are hopeless. */
        private int codeLength = 6;
        private int ttlMinutes = 10;
        /** A 6-digit code is only safe while guessing is capped. */
        private int maxAttempts = 5;
        /** Stops someone using "resend" to flood an inbox they do not own. */
        private int resendCooldownSeconds = 60;
    }
}
