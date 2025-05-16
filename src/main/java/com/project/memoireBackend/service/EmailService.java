package com.project.memoireBackend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ConditionalOnProperty(name = "app.email.enabled", havingValue = "true", matchIfMissing = false)
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@example.com}")
    private String fromEmail;

    @Value("${app.alerts.recipients:admin@example.com}")
    private List<String> alertRecipients;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendAlertEmail(String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(alertRecipients.toArray(new String[0]));
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            System.out.println("Email d'alerte envoyé: " + subject);
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi de l'email: " + e.getMessage());
        }
    }

    public void sendAlertEmailToSpecificRecipients(String subject, String body, List<String> recipients) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(recipients.toArray(new String[0]));
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            System.out.println("Email d'alerte envoyé à: " + recipients);
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi de l'email: " + e.getMessage());
        }
    }
}