package com.project.memoireBackend.service;

import com.project.memoireBackend.dto.DatabaseMetric;
import com.project.memoireBackend.model.DatabaseInstance;
import com.project.memoireBackend.model.MetricType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AlertService {

    // Utiliser une interface commune pour les deux services d'email
    private final Object emailService;

    @Autowired
    private ActivityLogService activityLogService;

    // Constructeur qui accepte soit EmailService soit LoggingEmailService
    public AlertService(@Autowired(required = false) EmailService realEmailService,
                        @Autowired(required = false) LoggingEmailService loggingEmailService) {
        // Utiliser le premier service disponible
        this.emailService = realEmailService != null ? realEmailService : loggingEmailService;
    }

    private static class AlertThreshold {
        MetricType metricType;
        String metricName;
        double warningThreshold;
        double criticalThreshold;

        public AlertThreshold(MetricType type, String name, double warning, double critical) {
            this.metricType = type;
            this.metricName = name;
            this.warningThreshold = warning;
            this.criticalThreshold = critical;
        }
    }

    private static final List<AlertThreshold> ALERT_THRESHOLDS = List.of(
            new AlertThreshold(MetricType.CONNECTIONS, "Active Sessions", 80, 100),
            new AlertThreshold(MetricType.TABLESPACE, "*Usage", 80, 95),
            new AlertThreshold(MetricType.MEMORY_USAGE, "*", 85, 95),
            new AlertThreshold(MetricType.QUERY_PERFORMANCE, "Buffer Cache Hit Ratio", 70, 60),
            new AlertThreshold(MetricType.QUERY_PERFORMANCE, "Long Running Queries", 5, 10)
    );

    public void checkMetricForAlerts(DatabaseMetric metric, DatabaseInstance database) {
        for (AlertThreshold threshold : ALERT_THRESHOLDS) {
            if (threshold.metricType == metric.getMetricType() &&
                    matchesMetricName(metric.getMetricName(), threshold.metricName)) {

                checkThreshold(metric, database, threshold);
            }
        }
    }

    private boolean matchesMetricName(String metricName, String pattern) {
        if (pattern.equals("*")) {
            return true;
        }
        if (pattern.startsWith("*")) {
            return metricName.endsWith(pattern.substring(1));
        }
        if (pattern.endsWith("*")) {
            return metricName.startsWith(pattern.substring(0, pattern.length() - 1));
        }
        return metricName.equals(pattern);
    }

    private void checkThreshold(DatabaseMetric metric, DatabaseInstance database, AlertThreshold threshold) {
        double value = metric.getMetricValue();
        String alertLevel = null;

        // Pour certaines métriques, on veut alerter quand la valeur est basse (comme le hit ratio)
        boolean isInverseMetric = metric.getMetricName().contains("Hit Ratio");

        if (isInverseMetric) {
            if (value <= threshold.criticalThreshold) {
                alertLevel = "CRITICAL";
            } else if (value <= threshold.warningThreshold) {
                alertLevel = "WARNING";
            }
        } else {
            if (value >= threshold.criticalThreshold) {
                alertLevel = "CRITICAL";
            } else if (value >= threshold.warningThreshold) {
                alertLevel = "WARNING";
            }
        }

        if (alertLevel != null) {
            sendAlert(database, metric, alertLevel, threshold);
        }
    }

    private void sendAlert(DatabaseInstance database, DatabaseMetric metric, String alertLevel, AlertThreshold threshold) {
        String subject = String.format("[%s] Alerte %s - %s",
                alertLevel, database.getName(), metric.getMetricName());

        String message = String.format(
                "Une alerte %s a été déclenchée pour la base de données %s.\n\n" +
                        "Métrique: %s\n" +
                        "Valeur actuelle: %.2f %s\n" +
                        "Seuil d'avertissement: %.2f\n" +
                        "Seuil critique: %.2f\n" +
                        "Heure: %s\n\n" +
                        "Veuillez vérifier l'état de la base de données.",
                alertLevel,
                database.getName(),
                metric.getMetricName(),
                metric.getMetricValue(),
                metric.getUnit() != null ? metric.getUnit().getDisplayName() : "",
                threshold.warningThreshold,
                threshold.criticalThreshold,
                metric.getTimestamp()
        );

        // Envoyer l'email en utilisant le service disponible
        sendEmailAlert(subject, message);

        // Logger l'alerte
        activityLogService.logDatabaseActivity(
                "system",
                "ALERT_" + alertLevel,
                String.format("Alerte %s: %s = %.2f", alertLevel, metric.getMetricName(), metric.getMetricValue()),
                database.getId()
        );
    }

    private void sendEmailAlert(String subject, String message) {
        try {
            if (emailService instanceof EmailService) {
                ((EmailService) emailService).sendAlertEmail(subject, message);
            } else if (emailService instanceof LoggingEmailService) {
                ((LoggingEmailService) emailService).sendAlertEmail(subject, message);
            } else {
                // Fallback si aucun service n'est disponible
                System.out.println("ALERTE (console uniquement): " + subject);
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi de l'alerte: " + e.getMessage());
        }
    }

    // Méthode pour configurer des alertes personnalisées
    public void addCustomAlert(MetricType metricType, String metricName, double warningThreshold, double criticalThreshold) {
        ALERT_THRESHOLDS.add(new AlertThreshold(metricType, metricName, warningThreshold, criticalThreshold));
    }
}