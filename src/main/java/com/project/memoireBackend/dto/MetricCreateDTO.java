package com.project.memoireBackend.dto;

import com.project.memoireBackend.model.MetricType;
import com.project.memoireBackend.model.MetricUnit;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MetricCreateDTO {
    @NotNull(message = "L'ID de la base de données est obligatoire")
    private Long databaseId;

    @NotNull(message = "Le type de métrique est obligatoire")
    private MetricType metricType;

    @NotBlank(message = "Le nom de la métrique est obligatoire")
    private String metricName;

    public Long getDatabaseId() {
        return databaseId;
    }

    public void setDatabaseId(Long databaseId) {
        this.databaseId = databaseId;
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

    @NotNull(message = "La valeur de la métrique est obligatoire")
    private Double metricValue;

    private MetricUnit unit;
}