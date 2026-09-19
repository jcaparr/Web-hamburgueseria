package com.hamburguesas.auth;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {

    private Verification verification = new Verification();
    private RateLimit rateLimit = new RateLimit();
    private Google google = new Google();

    @Data
    public static class Google {
        /**
         * OAuth client id for the web app. Public by design: it ships inside the
         * frontend bundle. Empty disables signing in with Google entirely.
         */
        private String clientId = "";
    }

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

    @Data
    public static class RateLimit {
        /**
         * Everything under /api/auth from one address. Generous enough for a family
         * behind one connection, tight enough to make scripted guessing pointless.
         */
        private int perIpRequests = 30;
        private int perIpWindowMinutes = 15;

        /** Password guesses against one account, wherever they come from. */
        private int perEmailLoginAttempts = 10;
        private int perEmailLoginWindowMinutes = 15;

        /**
         * Emails we will send to one address per hour. The resend cooldown already
         * spaces them out; this caps the daily total someone can trigger.
         */
        private int perEmailSends = 5;
        private int perEmailSendWindowMinutes = 60;
    }
}
