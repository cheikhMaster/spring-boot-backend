package com.project.memoireBackend.model;


public enum DatabaseStatus {
    ACTIVE("Active"),
    INACTIVE("Inactive"),
    ERROR("Error"),
    MAINTENANCE("Maintenance");

    private final String displayName;

    DatabaseStatus(String displayName) {
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