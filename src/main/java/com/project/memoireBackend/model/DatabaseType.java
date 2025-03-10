package com.project.memoireBackend.model;

public enum DatabaseType {
    POSTGRES("PostgreSQL"),
    MYSQL("MySQL"),
    ORACLE("Oracle"),
    SQLSERVER("SQL Server"),
    MONGODB("MongoDB"),
    MARIADB("MariaDB");

    private final String displayName;

    DatabaseType(String displayName) {
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
