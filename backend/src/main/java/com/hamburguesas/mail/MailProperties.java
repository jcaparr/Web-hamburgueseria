package com.hamburguesas.mail;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.mail")
public class MailProperties {

    /** Address the emails are sent from. Must match the authenticated SMTP account. */
    private String from = "";

    private String fromName = "Hamburgueserías BA";
}
