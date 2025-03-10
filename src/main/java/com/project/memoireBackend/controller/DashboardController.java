package com.project.memoireBackend.controller;


import com.project.memoireBackend.dto.DashboardStatsDTO;
import com.project.memoireBackend.dto.MetricSummaryDTO;
import com.project.memoireBackend.model.BackupStatus;
import com.project.memoireBackend.model.DatabaseStatus;
import com.project.memoireBackend.model.MetricType;
import com.project.memoireBackend.service.BackupService;
import com.project.memoireBackend.service.DatabaseInstanceService;
import com.project.memoireBackend.service.MetricService;
import com.project.memoireBackend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DatabaseInstanceService databaseInstanceService;

    @Autowired
    private BackupService backupService;

    @Autowired
    private UserService userService;

    @Autowired
    private MetricService metricService;

    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsDTO> getDashboardStats() {
        // Calculer les statistiques
        long totalDatabases = databaseInstanceService.getAllDatabaseInstances().size();
        long activeDatabases = databaseInstanceService.getDatabaseInstancesByStatus(DatabaseStatus.ACTIVE).size();
        long errorDatabases = databaseInstanceService.getDatabaseInstancesByStatus(DatabaseStatus.ERROR).size();

        long totalBackups = backupService.getAllBackups().size();
        long pendingBackups = backupService.getBackupsByStatus(BackupStatus.PENDING).size() +
                backupService.getBackupsByStatus(BackupStatus.IN_PROGRESS).size();
        long failedBackups = backupService.getBackupsByStatus(BackupStatus.FAILED).size();

        long totalUsers = userService.getAllUsers().size();
        long activeUsers = userService.getActiveUsers().size();

        // Obtenir les métriques récentes (dernières 24 heures)
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(1);

        // Pour simplifier, supposons que nous avons des ID de base de données pour les métriques globales
        // Dans une implémentation réelle, on agrégerait les données de toutes les bases
        Long sampleDatabaseId = 1L;

        MetricSummaryDTO connectionMetrics = metricService.getMetricSummary(
                sampleDatabaseId, MetricType.CONNECTIONS, startDate, endDate);

        MetricSummaryDTO tablespaceMetrics = metricService.getMetricSummary(
                sampleDatabaseId, MetricType.TABLESPACE, startDate, endDate);

        MetricSummaryDTO ioMetrics = metricService.getMetricSummary(
                sampleDatabaseId, MetricType.IO_OPERATIONS, startDate, endDate);

        MetricSummaryDTO networkMetrics = metricService.getMetricSummary(
                sampleDatabaseId, MetricType.NETWORK_TRAFFIC, startDate, endDate);

        // Construire le DTO
        DashboardStatsDTO statsDTO = new DashboardStatsDTO(
                totalDatabases, activeDatabases, errorDatabases,
                totalBackups, pendingBackups, failedBackups,
                totalUsers, activeUsers,
                connectionMetrics, tablespaceMetrics, ioMetrics, networkMetrics);

        return ResponseEntity.ok(statsDTO);
    }
}