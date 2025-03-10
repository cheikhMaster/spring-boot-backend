package com.project.memoireBackend.model;

public enum MetricType {
    CONNECTIONS("Connexions simultanées"),
    TABLESPACE("Occupation tablespace"),
    IO_OPERATIONS("Opérations E/S disque"),
    NETWORK_TRAFFIC("Trafic réseau"),
    CPU_USAGE("Utilisation CPU"),
    MEMORY_USAGE("Utilisation mémoire"),
    QUERY_PERFORMANCE("Performance des requêtes");

    private final String displayName;

    MetricType(String displayName) {
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