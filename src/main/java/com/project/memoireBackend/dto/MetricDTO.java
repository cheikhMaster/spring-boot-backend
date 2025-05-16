package com.project.memoireBackend.dto;

import com.project.memoireBackend.model.MetricType;
import com.project.memoireBackend.model.MetricUnit;


import java.time.LocalDateTime;


public class MetricDTO {
    private Long id;
    private Long databaseId;
    private String databaseName;
    private LocalDateTime timestamp;
    private MetricType metricType;
    private String metricName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDatabaseId() {
        return databaseId;
    }

    public void setDatabaseId(Long databaseId) {
        this.databaseId = databaseId;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public MetricType getMetricType() {
        return metricType;
    }

    public void setMetricType(MetricType metricType) {
        this.metricType = metricType;
    }

    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    public Double getMetricValue() {
        return metricValue;
    }

    public void setMetricValue(Double metricValue) {
        this.metricValue = metricValue;
    }

    public MetricUnit getUnit() {
        return unit;
    }

    public void setUnit(MetricUnit unit) {
        this.unit = unit;
    }

    private Double metricValue;
    private MetricUnit unit;
}