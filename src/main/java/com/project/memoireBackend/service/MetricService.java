package com.project.memoireBackend.service;




import com.project.memoireBackend.dto.DatabaseMetric;
import com.project.memoireBackend.dto.MetricCreateDTO;
import com.project.memoireBackend.dto.MetricDTO;
import com.project.memoireBackend.dto.MetricSummaryDTO;
import com.project.memoireBackend.excepton.ResourceNotFoundException;
import com.project.memoireBackend.model.DatabaseInstance;
import com.project.memoireBackend.model.MetricType;
import com.project.memoireBackend.repository.DatabaseInstanceRepository;
import com.project.memoireBackend.repository.MetricRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class MetricService {

    @Autowired
    private MetricRepository metricRepository;

    @Autowired
    private DatabaseInstanceRepository databaseInstanceRepository;

    @Autowired
    private ActivityLogService activityLogService;

    public List<MetricDTO> getAllMetrics() {
        return metricRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public Page<MetricDTO> getMetricsPaged(Pageable pageable) {
        return metricRepository.findAll(pageable)
                .map(this::convertToDTO);
    }

    public Page<MetricDTO> getMetricsByDatabasePaged(Long databaseId, Pageable pageable) {
        DatabaseInstance instance = databaseInstanceRepository.findById(databaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Base de données non trouvée avec l'id: " + databaseId));

        return metricRepository.findByDatabaseInstance(instance, pageable)
                .map(this::convertToDTO);
    }

    public List<MetricDTO> getMetricsByType(MetricType type) {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(1);

        return metricRepository.findByMetricTypeAndTimestampBetween(type, startDate, endDate).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<MetricDTO> getMetricsByDatabaseAndType(Long databaseId, MetricType type, LocalDateTime start, LocalDateTime end) {
        DatabaseInstance instance = databaseInstanceRepository.findById(databaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Base de données non trouvée avec l'id: " + databaseId));

        return metricRepository.findByDatabaseInstanceAndMetricTypeAndTimestampBetween(instance, type, start, end).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public MetricDTO getLatestMetricByDatabaseAndType(Long databaseId, MetricType type) {
        DatabaseInstance instance = databaseInstanceRepository.findById(databaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Base de données non trouvée avec l'id: " + databaseId));

        DatabaseMetric metric = (DatabaseMetric) metricRepository.findLatestByDatabaseInstanceAndMetricType(instance, type);
        if (metric == null) {
            return null;
        }

        return convertToDTO(metric);
    }

    public MetricSummaryDTO getMetricSummary(Long databaseId, MetricType type, LocalDateTime start, LocalDateTime end) {
        DatabaseInstance instance = databaseInstanceRepository.findById(databaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Base de données non trouvée avec l'id: " + databaseId));

        List<DatabaseMetric> metrics = metricRepository.findByDatabaseInstanceAndMetricTypeAndTimestampBetween(
                instance, type, start, end);

        if (metrics.isEmpty()) {
            return new MetricSummaryDTO(0.0, 0.0, 0.0, 0.0, "unknown");
        }

        Double min = metrics.stream().mapToDouble(DatabaseMetric::getMetricValue).min().orElse(0);
        Double max = metrics.stream().mapToDouble(DatabaseMetric::getMetricValue).max().orElse(0);
        Double avg = metrics.stream().mapToDouble(DatabaseMetric::getMetricValue).average().orElse(0);

        // Trouver la valeur la plus récente
        DatabaseMetric latestMetric = metrics.stream()
                .max((m1, m2) -> m1.getTimestamp().compareTo(m2.getTimestamp()))
                .orElse(null);

        Double current = latestMetric != null ? latestMetric.getMetricValue() : 0.0;
        String unit = latestMetric != null && latestMetric.getUnit() != null ?
                latestMetric.getUnit().toString() : "unknown"; // Fournir une valeur par défaut

        return new MetricSummaryDTO(min, max, avg, current, unit);
    }



    @Transactional
    public MetricDTO createMetric(MetricCreateDTO metricCreateDTO, String username) {
        DatabaseInstance instance = databaseInstanceRepository.findById(metricCreateDTO.getDatabaseId())
                .orElseThrow(() -> new ResourceNotFoundException("Base de données non trouvée avec l'id: " + metricCreateDTO.getDatabaseId()));

        DatabaseMetric metric = new DatabaseMetric();
        metric.setDatabaseInstance(instance);
        metric.setTimestamp(LocalDateTime.now());
        metric.setMetricType(metricCreateDTO.getMetricType());
        metric.setMetricName(metricCreateDTO.getMetricName());
        metric.setMetricValue(metricCreateDTO.getMetricValue());
        metric.setUnit(metricCreateDTO.getUnit());

        DatabaseMetric savedMetric = metricRepository.save(metric);

        // Journalisation pour des changements significatifs
        if (isSignificantChange(metric)) {
            activityLogService.logMetricActivity(
                    username,
                    "RECORD_METRIC",
                    "Enregistrement d'une métrique anormale pour " + instance.getName() + ": " +
                            metric.getMetricType() + " - " + metric.getMetricValue(),
                    instance.getId()
            );
        }

        return convertToDTO(savedMetric);
    }

    private boolean isSignificantChange(DatabaseMetric metric) {
        // Logique pour déterminer si une métrique représente un changement significatif
        // qui doit être journalisé
        // Par exemple, si la valeur est au-delà d'un certain seuil

        switch (metric.getMetricType()) {
            case CONNECTIONS:
                return metric.getMetricValue() > 100; // Plus de 100 connexions simultanées
            case TABLESPACE:
                return metric.getMetricValue() > 90; // Utilisation du tablespace > 90%
            case IO_OPERATIONS:
                return metric.getMetricValue() > 1000; // Plus de 1000 opérations d'E/S par seconde
            case NETWORK_TRAFFIC:
                return metric.getMetricValue() > 100; // Trafic réseau > 100 Mbps
            default:
                return false;
        }
    }

    @Transactional
    public void deleteOldMetrics(LocalDateTime cutoffDate) {
        // Suppression des métriques plus anciennes que la date limite
        // Typiquement appelée par un job planifié

        List<DatabaseMetric> oldMetrics = metricRepository.findAll().stream()
                .filter(m -> m.getTimestamp().isBefore(cutoffDate))
                .collect(Collectors.toList());

        metricRepository.deleteAll(oldMetrics);

        // Pas de journalisation pour cette opération de maintenance
    }

    private MetricDTO convertToDTO(DatabaseMetric metric) {
        MetricDTO dto = new MetricDTO();
        dto.setId(metric.getId());
        dto.setDatabaseId(metric.getDatabaseInstance().getId());
        dto.setDatabaseName(metric.getDatabaseInstance().getName());
        dto.setTimestamp(metric.getTimestamp());
        dto.setMetricType(metric.getMetricType());
        dto.setMetricName(metric.getMetricName());
        dto.setMetricValue(metric.getMetricValue());
        dto.setUnit(metric.getUnit());
        return dto;
    }
}