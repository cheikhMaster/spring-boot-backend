package com.project.memoireBackend.dto;

import com.project.memoireBackend.model.NetworkStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NetworkConnectionDTO {
    private Long id;
    private Long sourceDatabaseId;
    private String sourceDatabaseName;
    private Long targetDatabaseId;
    private String targetDatabaseName;
    private LocalDateTime timestamp;
    private Double latency;
    private Double throughput;
    private Long packetLoss;
    private NetworkStatus status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSourceDatabaseId() {
        return sourceDatabaseId;
    }

    public void setSourceDatabaseId(Long sourceDatabaseId) {
        this.sourceDatabaseId = sourceDatabaseId;
    }

    public String getSourceDatabaseName() {
        return sourceDatabaseName;
    }

    public void setSourceDatabaseName(String sourceDatabaseName) {
        this.sourceDatabaseName = sourceDatabaseName;
    }

    public Long getTargetDatabaseId() {
        return targetDatabaseId;
    }

    public void setTargetDatabaseId(Long targetDatabaseId) {
        this.targetDatabaseId = targetDatabaseId;
    }

    public String getTargetDatabaseName() {
        return targetDatabaseName;
    }

    public void setTargetDatabaseName(String targetDatabaseName) {
        this.targetDatabaseName = targetDatabaseName;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Double getLatency() {
        return latency;
    }

    public void setLatency(Double latency) {
        this.latency = latency;
    }

    public Double getThroughput() {
        return throughput;
    }

    public void setThroughput(Double throughput) {
        this.throughput = throughput;
    }

    public Long getPacketLoss() {
        return packetLoss;
    }

    public void setPacketLoss(Long packetLoss) {
        this.packetLoss = packetLoss;
    }

    public NetworkStatus getStatus() {
        return status;
    }

    public void setStatus(NetworkStatus status) {
        this.status = status;
    }
}