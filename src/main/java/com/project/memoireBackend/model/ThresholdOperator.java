package com.project.memoireBackend.model;

public enum ThresholdOperator {
    GREATER_THAN("Supérieur à"),
    LESS_THAN("Inférieur à"),
    EQUALS("Égal à"),
    NOT_EQUALS("Différent de");

    private final String displayName;

    ThresholdOperator(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}