package com.project.memoireBackend.repository;




import com.project.memoireBackend.dto.DatabaseMetric;
import com.project.memoireBackend.model.DatabaseInstance;
import com.project.memoireBackend.model.MetricType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MetricRepository extends JpaRepository<DatabaseMetric, Long> {
    List<DatabaseMetric> findByDatabaseInstanceAndMetricTypeAndTimestampBetween(
            DatabaseInstance databaseInstance, MetricType metricType,
            LocalDateTime start, LocalDateTime end);

    List<DatabaseMetric> findByMetricTypeAndTimestampBetween(
            MetricType metricType, LocalDateTime start, LocalDateTime end);

    Page<DatabaseMetric> findByDatabaseInstance(DatabaseInstance databaseInstance, Pageable pageable);

    @Query("SELECT m FROM DatabaseMetric m WHERE m.databaseInstance = :instance AND m.metricType = :type " +
            "AND m.timestamp = (SELECT MAX(m2.timestamp) FROM DatabaseMetric m2 WHERE m2.databaseInstance = :instance AND m2.metricType = :type)")
    DatabaseMetric findLatestByDatabaseInstanceAndMetricType(DatabaseInstance instance, MetricType type);

    @Query("SELECT AVG(m.metricValue) FROM DatabaseMetric m WHERE m.databaseInstance = :instance AND m.metricType = :type " +
            "AND m.timestamp BETWEEN :start AND :end")
    Double findAverageValueByDatabaseInstanceAndMetricTypeBetween(
            DatabaseInstance instance, MetricType type, LocalDateTime start, LocalDateTime end);
}
