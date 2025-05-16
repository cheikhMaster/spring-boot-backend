package com.project.memoireBackend.service;

import com.project.memoireBackend.dto.MetricCreateDTO;
import com.project.memoireBackend.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@Service
public class MySQLMetricCollector {

    @Autowired
    private MetricService metricService;

    @Autowired
    private ActivityLogService activityLogService;

    public void collectMetrics(DatabaseInstance instance) {
        if (instance.getType() != DatabaseType.MYSQL) {
            return;
        }

        Connection connection = null;
        try {
            // Établir la connexion
            String url = "jdbc:mysql://" + instance.getAddress() + ":" + instance.getPort() + "/";
            connection = DriverManager.getConnection(url, instance.getUsername(), instance.getPassword());

            // Collecter différents types de métriques
            collectConnectionMetrics(connection, instance);
            collectTablespaceMetrics(connection, instance);
            collectIOMetrics(connection, instance);
            collectPerformanceMetrics(connection, instance);

        } catch (Exception e) {
            System.err.println("Erreur lors de la collecte des métriques MySQL: " + e.getMessage());
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

    private void collectConnectionMetrics(Connection connection, DatabaseInstance instance) throws Exception {
        // Nombre de connexions actives
        String query = "SHOW STATUS LIKE 'Threads_connected'";

        try (PreparedStatement ps = connection.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                int activeConnections = rs.getInt("Value");

                MetricCreateDTO metric = new MetricCreateDTO();
                metric.setDatabaseId(instance.getId());
                metric.setMetricType(MetricType.CONNECTIONS);
                metric.setMetricName("Active Connections");
                metric.setMetricValue((double) activeConnections);
                metric.setUnit(MetricUnit.COUNT);

                metricService.createMetric(metric, "system");
            }
        }

        // Nombre maximum de connexions
        String maxQuery = "SHOW VARIABLES LIKE 'max_connections'";

        try (PreparedStatement ps = connection.prepareStatement(maxQuery);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                int maxConnections = rs.getInt("Value");

                MetricCreateDTO metric = new MetricCreateDTO();
                metric.setDatabaseId(instance.getId());
                metric.setMetricType(MetricType.CONNECTIONS);
                metric.setMetricName("Max Connections");
                metric.setMetricValue((double) maxConnections);
                metric.setUnit(MetricUnit.COUNT);

                metricService.createMetric(metric, "system");
            }
        }
    }

    private void collectTablespaceMetrics(Connection connection, DatabaseInstance instance) throws Exception {
        String query =
                "SELECT " +
                        "  table_schema AS database_name, " +
                        "  ROUND(SUM(data_length + index_length) / 1048576, 2) AS size_mb, " +
                        "  ROUND(SUM(data_free) / 1048576, 2) AS free_mb " +
                        "FROM information_schema.tables " +
                        "GROUP BY table_schema";

        try (PreparedStatement ps = connection.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String databaseName = rs.getString("database_name");
                double sizeMb = rs.getDouble("size_mb");
                double freeMb = rs.getDouble("free_mb");

                // Métrique de taille
                MetricCreateDTO sizeMetric = new MetricCreateDTO();
                sizeMetric.setDatabaseId(instance.getId());
                sizeMetric.setMetricType(MetricType.TABLESPACE);
                sizeMetric.setMetricName("Database " + databaseName + " Size");
                sizeMetric.setMetricValue(sizeMb);
                sizeMetric.setUnit(MetricUnit.MEGABYTES);

                metricService.createMetric(sizeMetric, "system");

                // Métrique d'espace libre
                MetricCreateDTO freeMetric = new MetricCreateDTO();
                freeMetric.setDatabaseId(instance.getId());
                freeMetric.setMetricType(MetricType.TABLESPACE);
                freeMetric.setMetricName("Database " + databaseName + " Free Space");
                freeMetric.setMetricValue(freeMb);
                freeMetric.setUnit(MetricUnit.MEGABYTES);

                metricService.createMetric(freeMetric, "system");
            }
        }
    }

    private void collectIOMetrics(Connection connection, DatabaseInstance instance) throws Exception {
        // Lectures et écritures InnoDB
        String ioQuery = "SHOW STATUS LIKE 'Innodb_%'";

        try (PreparedStatement ps = connection.prepareStatement(ioQuery);
             ResultSet rs = ps.executeQuery()) {

            double dataReads = 0;
            double dataWrites = 0;

            while (rs.next()) {
                String variableName = rs.getString("Variable_name");
                double value = rs.getDouble("Value");

                if ("Innodb_data_reads".equals(variableName)) {
                    dataReads = value;
                } else if ("Innodb_data_writes".equals(variableName)) {
                    dataWrites = value;
                }
            }

            // Métrique des lectures
            MetricCreateDTO readsMetric = new MetricCreateDTO();
            readsMetric.setDatabaseId(instance.getId());
            readsMetric.setMetricType(MetricType.IO_OPERATIONS);
            readsMetric.setMetricName("Data Reads");
            readsMetric.setMetricValue(dataReads);
            readsMetric.setUnit(MetricUnit.COUNT);

            metricService.createMetric(readsMetric, "system");

            // Métrique des écritures
            MetricCreateDTO writesMetric = new MetricCreateDTO();
            writesMetric.setDatabaseId(instance.getId());
            writesMetric.setMetricType(MetricType.IO_OPERATIONS);
            writesMetric.setMetricName("Data Writes");
            writesMetric.setMetricValue(dataWrites);
            writesMetric.setUnit(MetricUnit.COUNT);

            metricService.createMetric(writesMetric, "system");
        }
    }

    private void collectPerformanceMetrics(Connection connection, DatabaseInstance instance) throws Exception {
        // Query Cache Hit Rate
        String cacheQuery = "SHOW STATUS LIKE 'Qcache%'";

        try (PreparedStatement ps = connection.prepareStatement(cacheQuery);
             ResultSet rs = ps.executeQuery()) {

            double hits = 0;
            double inserts = 0;

            while (rs.next()) {
                String variableName = rs.getString("Variable_name");
                double value = rs.getDouble("Value");

                if ("Qcache_hits".equals(variableName)) {
                    hits = value;
                } else if ("Qcache_inserts".equals(variableName)) {
                    inserts = value;
                }
            }

            // Calculer le hit ratio
            double hitRatio = 0;
            if (hits + inserts > 0) {
                hitRatio = (hits / (hits + inserts)) * 100;
            }

            MetricCreateDTO metric = new MetricCreateDTO();
            metric.setDatabaseId(instance.getId());
            metric.setMetricType(MetricType.QUERY_PERFORMANCE);
            metric.setMetricName("Query Cache Hit Ratio");
            metric.setMetricValue(hitRatio);
            metric.setUnit(MetricUnit.PERCENTAGE);

            metricService.createMetric(metric, "system");
        }

        // Slow queries
        String slowQuery = "SHOW STATUS LIKE 'Slow_queries'";

        try (PreparedStatement ps = connection.prepareStatement(slowQuery);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                double slowQueries = rs.getDouble("Value");

                MetricCreateDTO metric = new MetricCreateDTO();
                metric.setDatabaseId(instance.getId());
                metric.setMetricType(MetricType.QUERY_PERFORMANCE);
                metric.setMetricName("Slow Queries");
                metric.setMetricValue(slowQueries);
                metric.setUnit(MetricUnit.COUNT);

                metricService.createMetric(metric, "system");
            }
        }
    }
}