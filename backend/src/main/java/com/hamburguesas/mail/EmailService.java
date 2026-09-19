package com.hamburguesas.mail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Sends the plain-text emails the auth flows depend on.
 *
 * Spring only creates a JavaMailSender when SMTP is configured, so on a developer
 * machine there is none. Rather than failing, the message is written to the log:
 * the whole point of these emails is a code the developer needs to read anyway.
 * That fallback only ever runs when no SMTP server is configured, which is never
 * the case in production.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final MailProperties properties;

    public void send(String to, String subject, String body) {
        JavaMailSender sender = mailSenderProvider.getIfAvailable();

        if (sender == null) {
            log.warn("""
                
                ========= EMAIL (sin SMTP configurado, no se envió) =========
                Para    : {}
                Asunto  : {}
                
                {}
                =============================================================
                """, to, subject, body);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(String.format("%s <%s>", properties.getFromName(), properties.getFrom()));
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        try {
            sender.send(message);
            // The address is not logged: it would put every user's email in the log file.
            log.info("Email sent: {}", subject);
        } catch (MailException ex) {
            log.error("Could not send email '{}': {}", subject, ex.getMessage());
            throw new EmailDeliveryException("No se pudo enviar el email", ex);
        }
    }
}
