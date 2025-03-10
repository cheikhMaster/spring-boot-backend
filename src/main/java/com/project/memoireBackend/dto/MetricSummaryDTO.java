package com.project.memoireBackend.dto;




public class MetricSummaryDTO {
    private Double minValue;
    private Double maxValue;
    private Double avgValue;
    private Double currentValue;
    private String unit;

    public MetricSummaryDTO(Double minValue, Double maxValue, Double avgValue, Double currentValue, String unit) {
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.avgValue = avgValue;
        this.currentValue = currentValue;
        this.unit = unit;
    }

    public Double getMinValue() {
        return minValue;
    }

    public void setMinValue(Double minValue) {
        this.minValue = minValue;
    }

    public Double getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(Double maxValue) {
        this.maxValue = maxValue;
    }

    public Double getAvgValue() {
        return avgValue;
    }

    public void setAvgValue(Double avgValue) {
        this.avgValue = avgValue;
    }

    public Double getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(Double currentValue) {
        this.currentValue = currentValue;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }
}