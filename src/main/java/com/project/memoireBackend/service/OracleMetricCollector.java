package com.project.memoireBackend.service;

import com.project.memoireBackend.dto.DatabaseMetric;
import com.project.memoireBackend.dto.MetricCreateDTO;
import com.project.memoireBackend.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OracleMetricCollector {

    @Autowired
    private MetricService metricService;

    @Autowired
    private ActivityLogService activityLogService;

    public void collectMetrics(DatabaseInstance instance) {
        if (instance.getType() != DatabaseType.ORACLE) {
            return;
        }

        Connection connection = null;
        try {
            // Établir la connexion
            String url = buildOracleUrl(instance);
            connection = DriverManager.getConnection(url, instance.getUsername(), instance.getPassword());

            // Collecter différents types de métriques
            collectConnectionMetrics(connection, instance);
            collectTablespaceMetrics(connection, instance);
            collectIOMetrics(connection, instance);
            collectMemoryMetrics(connection, instance);
            collectPerformanceMetrics(connection, instance);

        } catch (Exception e) {
            System.err.println("Erreur lors de la collecte des métriques Oracle: " + e.getMessage());
            // Log l'erreur
            activityLogService.logDatabaseActivity(
                    "system",
                    "MONITOR_ERROR",
                    "Erreur de collecte des métriques pour " + instance.getName() + ": " + e.getMessage(),
                    instance.getId()
            );
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String buildOracleUrl(DatabaseInstance instance) {
        if (instance.getSid() != null && !instance.getSid().isEmpty()) {
            return "jdbc:oracle:thin:@" + instance.getAddress() + ":" + instance.getPort() + ":" + instance.getSid();
        } else if (instance.getServiceName() != null && !instance.getServiceName().isEmpty()) {
            return "jdbc:oracle:thin:@//" + instance.getAddress() + ":" + instance.getPort() + "/" + instance.getServiceName();
        }
        return null;
    }

    private void collectConnectionMetrics(Connection connection, DatabaseInstance instance) throws Exception {
        String query = "SELECT COUNT(*) as active_sessions FROM v$session WHERE status = 'ACTIVE' AND username IS NOT NULL";

        try (PreparedStatement ps = connection.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                int activeSessions = rs.getInt("active_sessions");

                MetricCreateDTO metric = new MetricCreateDTO();
                metric.setDatabaseId(instance.getId());
                metric.setMetricType(MetricType.CONNECTIONS);
                metric.setMetricName("Active Sessions");
                metric.setMetricValue((double) activeSessions);
                metric.setUnit(MetricUnit.COUNT);

                metricService.createMetric(metric, "system");
            }
        }

        // Collecter aussi le nombre total de sessions
        String totalQuery = "SELECT COUNT(*) as total_sessions FROM v$session WHERE username IS NOT NULL";

        try (PreparedStatement ps = connection.prepareStatement(totalQuery);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                int totalSessions = rs.getInt("total_sessions");

                MetricCreateDTO metric = new MetricCreateDTO();
                metric.setDatabaseId(instance.getId());
                metric.setMetricType(MetricType.CONNECTIONS);
                metric.setMetricName("Total Sessions");
                metric.setMetricValue((double) totalSessions);
                metric.setUnit(MetricUnit.COUNT);

                metricService.createMetric(metric, "system");
            }
        }
    }

    private void collectTablespaceMetrics(Connection connection, DatabaseInstance instance) throws Exception {
        String query =
                "SELECT " +
                        "  tablespace_name, " +
                        "  ROUND(used_space * 8192 / 1048576, 2) as used_mb, " +
                        "  ROUND(tablespace_size * 8192 / 1048576, 2) as total_mb, " +
                        "  ROUND(used_percent, 2) as used_percent " +
                        "FROM dba_tablespace_usage_metrics " +
                        "ORDER BY used_percent DESC";

        try (PreparedStatement ps = connection.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String tablespaceName = rs.getString("tablespace_name");
                double usedMb = rs.getDouble("used_mb");
                double totalMb = rs.getDouble("total_mb");
                double usedPercent = rs.getDouble("used_percent");

                // Métrique d'utilisation en pourcentage
                MetricCreateDTO percentMetric = new MetricCreateDTO();
                percentMetric.setDatabaseId(instance.getId());
                percentMetric.setMetricType(MetricType.TABLESPACE);
                percentMetric.setMetricName("Tablespace " + tablespaceName + " Usage");
                percentMetric.setMetricValue(usedPercent);
                percentMetric.setUnit(MetricUnit.PERCENTAGE);

                metricService.createMetric(percentMetric, "system");

                // Métrique d'utilisation en MB
                MetricCreateDTO sizeMetric = new MetricCreateDTO();
                sizeMetric.setDatabaseId(instance.getId());
                sizeMetric.setMetricType(MetricType.TABLESPACE);
                sizeMetric.setMetricName("Tablespace " + tablespaceName + " Size");
                sizeMetric.setMetricValue(usedMb);
                sizeMetric.setUnit(MetricUnit.MEGABYTES);

                metricService.createMetric(sizeMetric, "system");
            }
        }
    }

    private void collectIOMetrics(Connection connection, DatabaseInstance instance) throws Exception {
        // Métriques d'I/O par fichier
        String fileIOQuery =
                "SELECT " +
                        "  f.file_name, " +
                        "  s.phyrds as physical_reads, " +
                        "  s.phywrts as physical_writes, " +
                        "  s.readtim as read_time, " +
                        "  s.writetim as write_time " +
                        "FROM v$filestat s " +
                        "JOIN dba_data_files f ON s.file# = f.file_id " +
                        "WHERE s.phyrds > 0 OR s.phywrts > 0";

        try (PreparedStatement ps = connection.prepareStatement(fileIOQuery);
             ResultSet rs = ps.executeQuery()) {

            double totalReads = 0;
            double totalWrites = 0;

            while (rs.next()) {
                totalReads += rs.getDouble("physical_reads");
                totalWrites += rs.getDouble("physical_writes");
            }

            // Métrique des lectures physiques
            MetricCreateDTO readsMetric = new MetricCreateDTO();
            readsMetric.setDatabaseId(instance.getId());
            readsMetric.setMetricType(MetricType.IO_OPERATIONS);
            readsMetric.setMetricName("Physical Reads");
            readsMetric.setMetricValue(totalReads);
            readsMetric.setUnit(MetricUnit.COUNT);

            metricService.createMetric(readsMetric, "system");

            // Métrique des écritures physiques
            MetricCreateDTO writesMetric = new MetricCreateDTO();
            writesMetric.setDatabaseId(instance.getId());
            writesMetric.setMetricType(MetricType.IO_OPERATIONS);
            writesMetric.setMetricName("Physical Writes");
            writesMetric.setMetricValue(totalWrites);
            writesMetric.setUnit(MetricUnit.COUNT);

            metricService.createMetric(writesMetric, "system");
        }
    }

    private void collectMemoryMetrics(Connection connection, DatabaseInstance instance) throws Exception {
        // SGA (System Global Area) metrics
        String sgaQuery =
                "SELECT " +
                        "  name, " +
                        "  ROUND(bytes/1048576, 2) as size_mb " +
                        "FROM v$sgainfo " +
                        "WHERE name IN ('Fixed SGA Size', 'Redo Buffers', 'Buffer Cache Size', 'Shared Pool Size')";

        try (PreparedStatement ps = connection.prepareStatement(sgaQuery);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String componentName = rs.getString("name");
                double sizeMb = rs.getDouble("size_mb");

                MetricCreateDTO metric = new MetricCreateDTO();
                metric.setDatabaseId(instance.getId());
                metric.setMetricType(MetricType.MEMORY_USAGE);
                metric.setMetricName(componentName);
                metric.setMetricValue(sizeMb);
                metric.setUnit(MetricUnit.MEGABYTES);

                metricService.createMetric(metric, "system");
            }
        }

        // PGA (Program Global Area) metrics
        String pgaQuery =
                "SELECT " +
                        "  ROUND(value/1048576, 2) as pga_size_mb " +
                        "FROM v$pgastat " +
                        "WHERE name = 'total PGA allocated'";

        try (PreparedStatement ps = connection.prepareStatement(pgaQuery);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                double pgaSizeMb = rs.getDouble("pga_size_mb");

                MetricCreateDTO metric = new MetricCreateDTO();
                metric.setDatabaseId(instance.getId());
                metric.setMetricType(MetricType.MEMORY_USAGE);
                metric.setMetricName("PGA Size");
                metric.setMetricValue(pgaSizeMb);
                metric.setUnit(MetricUnit.MEGABYTES);

                metricService.createMetric(metric, "system");
            }
        }
    }

    private void collectPerformanceMetrics(Connection connection, DatabaseInstance instance) throws Exception {
        // Buffer Cache Hit Ratio
        String bufferHitQuery =
                "SELECT " +
                        "  ROUND((1 - (phy.value - lob.value - dir.value) / (ses.value + con.value - lob.value - dir.value)) * 100, 2) as hit_ratio " +
                        "FROM v$sysstat phy, v$sysstat lob, v$sysstat dir, v$sysstat ses, v$sysstat con " +
                        "WHERE phy.name = 'physical reads' " +
                        "AND lob.name = 'physical reads direct (lob)' " +
                        "AND dir.name = 'physical reads direct' " +
                        "AND ses.name = 'session logical reads' " +
                        "AND con.name = 'consistent gets direct'";

        try (PreparedStatement ps = connection.prepareStatement(bufferHitQuery);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                double hitRatio = rs.getDouble("hit_ratio");

                MetricCreateDTO metric = new MetricCreateDTO();
                metric.setDatabaseId(instance.getId());
                metric.setMetricType(MetricType.QUERY_PERFORMANCE);
                metric.setMetricName("Buffer Cache Hit Ratio");
                metric.setMetricValue(hitRatio);
                metric.setUnit(MetricUnit.PERCENTAGE);

                metricService.createMetric(metric, "system");
            }
        }

        // Requêtes actives longues
        String longQueriesQuery =
                "SELECT COUNT(*) as long_queries " +
                        "FROM v$session " +
                        "WHERE status = 'ACTIVE' " +
                        "AND last_call_et > 300 " + // Plus de 5 minutes
                        "AND username IS NOT NULL";

        try (PreparedStatement ps = connection.prepareStatement(longQueriesQuery);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                int longQueries = rs.getInt("long_queries");

                MetricCreateDTO metric = new MetricCreateDTO();
                metric.setDatabaseId(instance.getId());
                metric.setMetricType(MetricType.QUERY_PERFORMANCE);
                metric.setMetricName("Long Running Queries");
                metric.setMetricValue((double) longQueries);
                metric.setUnit(MetricUnit.COUNT);

                metricService.createMetric(metric, "system");
            }
        }
    }
}