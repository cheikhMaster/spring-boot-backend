package com.project.memoireBackend.model;

public enum MetricUnit {
    COUNT("Nombre"),
    PERCENTAGE("Pourcentage"),
    BYTES("Octets"),
    KILOBYTES("Ko"),
    MEGABYTES("Mo"),
    GIGABYTES("Go"),
    MILLISECONDS("ms"),
    SECONDS("s"),
    OPERATIONS_PER_SECOND("Opérations/s"),
    MEGABITS_PER_SECOND("Mbps");

    private final String displayName;

    MetricUnit(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}