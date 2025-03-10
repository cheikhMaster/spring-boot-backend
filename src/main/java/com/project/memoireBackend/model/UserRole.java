package com.project.memoireBackend.model;

public enum UserRole {
    ADMIN,
    USER; // Viewer

    @Override
    public String toString() {
        return name();
    }
}
