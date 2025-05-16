package com.project.memoireBackend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ConditionalOnProperty(name = "app.email.enabled", havingValue = "false", matchIfMissing = true)
public class LoggingEmailService {

    private static final Logger logger = LoggerFactory.getLogger(LoggingEmailService.class);

    public void sendAlertEmail(String subject, String body) {
        logger.info("ALERTE EMAIL (log uniquement): {} - {}", subject, body);
    }

    public void sendAlertEmailToSpecificRecipients(String subject, String body, List<String> recipients) {
        logger.info("ALERTE EMAIL pour {}: {} - {}", recipients, subject, body);
    }
}