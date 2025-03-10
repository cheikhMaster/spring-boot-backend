package com.project.memoireBackend.model;

public enum BackupStatus {
    PENDING("En attente"),
    QUEUED("En file d'attente"),
    IN_PROGRESS("En cours"),
    COMPLETED("Terminé"),
    FAILED("Échoué"),
    CANCELED("Annulé");

    private final String displayName;

    BackupStatus(String displayName) {
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