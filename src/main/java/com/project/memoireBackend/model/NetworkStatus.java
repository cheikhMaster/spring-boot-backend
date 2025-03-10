package com.project.memoireBackend.model;

public enum NetworkStatus {
    ACTIVE("Active"),
    DEGRADED("Dégradée"),
    DOWN("Interrompue"),
    UNSTABLE("Instable");

    private final String displayName;

    NetworkStatus(String displayName) {
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