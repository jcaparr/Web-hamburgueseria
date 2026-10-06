package com.hamburguesas.mail;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.mail")
public class MailProperties {

    /**
     * Whether email is actually sent. Off means every message is written to the log
     * instead, which is fine on a developer machine and unacceptable anywhere else:
     * the log would hold the codes that activate accounts.
     *
     * It is an explicit switch rather than "is SMTP configured?" because an empty
     * environment variable is not the same as a missing one, and docker-compose sets
     * empty ones. Deciding by accident is how production ends up quietly logging
     * codes instead of emailing them.
     */
    private boolean enabled = false;

    /** Address the emails are sent from. Must match the authenticated SMTP account. */
    private String from = "";

    private String fromName = "Burgómetro";
}
