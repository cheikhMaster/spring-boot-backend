package com.project.memoireBackend.model;

public enum BackupType {
    FULL("Complète"),
    INCREMENTAL("Incrémentale"),
    DIFFERENTIAL("Différentielle"),
    TRANSACTION_LOG("Journal de transactions");

    private final String displayName;

    BackupType(String displayName) {
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