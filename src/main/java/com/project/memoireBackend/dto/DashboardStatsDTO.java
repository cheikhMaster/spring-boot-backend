package com.project.memoireBackend.dto;

public class DashboardStatsDTO {
    private long totalDatabases;
    private long activeDatabases;
    private long errorDatabases;
    private long totalBackups;
    private long pendingBackups;
    private long failedBackups;
    private long totalUsers;
    private long activeUsers;

    private MetricSummaryDTO connectionMetrics;
    private MetricSummaryDTO tablespaceMetrics;
    private MetricSummaryDTO ioMetrics;
    private MetricSummaryDTO networkMetrics;

    // Constructeur avec tous les arguments nécessaires
    public DashboardStatsDTO(long totalDatabases, long activeDatabases, long errorDatabases,
                             long totalBackups, long pendingBackups, long failedBackups,
                             long totalUsers, long activeUsers,
                             MetricSummaryDTO connectionMetrics, MetricSummaryDTO tablespaceMetrics,
                             MetricSummaryDTO ioMetrics, MetricSummaryDTO networkMetrics) {
        this.totalDatabases = totalDatabases;
        this.activeDatabases = activeDatabases;
        this.errorDatabases = errorDatabases;
        this.totalBackups = totalBackups;
        this.pendingBackups = pendingBackups;
        this.failedBackups = failedBackups;
        this.totalUsers = totalUsers;
        this.activeUsers = activeUsers;
        this.connectionMetrics = connectionMetrics;
        this.tablespaceMetrics = tablespaceMetrics;
        this.ioMetrics = ioMetrics;
        this.networkMetrics = networkMetrics;
    }

    // Getters et Setters
    public long getTotalDatabases() {
        return totalDatabases;
    }

    public void setTotalDatabases(long totalDatabases) {
        this.totalDatabases = totalDatabases;
    }

    public long getActiveDatabases() {
        return activeDatabases;
    }

    public void setActiveDatabases(long activeDatabases) {
        this.activeDatabases = activeDatabases;
    }

    public long getErrorDatabases() {
        return errorDatabases;
    }

    public void setErrorDatabases(long errorDatabases) {
        this.errorDatabases = errorDatabases;
    }

    public long getTotalBackups() {
        return totalBackups;
    }

    public void setTotalBackups(long totalBackups) {
        this.totalBackups = totalBackups;
    }

    public long getPendingBackups() {
        return pendingBackups;
    }

    public void setPendingBackups(long pendingBackups) {
        this.pendingBackups = pendingBackups;
    }

    public long getFailedBackups() {
        return failedBackups;
    }

    public void setFailedBackups(long failedBackups) {
        this.failedBackups = failedBackups;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getActiveUsers() {
        return activeUsers;
    }

    public void setActiveUsers(long activeUsers) {
        this.activeUsers = activeUsers;
    }

    public MetricSummaryDTO getConnectionMetrics() {
        return connectionMetrics;
    }

    public void setConnectionMetrics(MetricSummaryDTO connectionMetrics) {
        this.connectionMetrics = connectionMetrics;
    }

    public MetricSummaryDTO getTablespaceMetrics() {
        return tablespaceMetrics;
    }

    public void setTablespaceMetrics(MetricSummaryDTO tablespaceMetrics) {
        this.tablespaceMetrics = tablespaceMetrics;
    }

    public MetricSummaryDTO getIoMetrics() {
        return ioMetrics;
    }

    public void setIoMetrics(MetricSummaryDTO ioMetrics) {
        this.ioMetrics = ioMetrics;
    }

    public MetricSummaryDTO getNetworkMetrics() {
        return networkMetrics;
    }

    public void setNetworkMetrics(MetricSummaryDTO networkMetrics) {
        this.networkMetrics = networkMetrics;
    }
}
