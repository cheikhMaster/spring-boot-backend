package com.project.memoireBackend.dto;

import com.project.memoireBackend.model.NetworkStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NetworkConnectionCreateDTO {
    @NotNull(message = "L'ID de la base de données source est obligatoire")
    private Long sourceDatabaseId;

    @NotNull(message = "L'ID de la base de données cible est obligatoire")
    private Long targetDatabaseId;

    private Double latency;
    private Double throughput;
    private Long packetLoss;

    public Long getSourceDatabaseId() {
        return sourceDatabaseId;
    }

    public void setSourceDatabaseId(Long sourceDatabaseId) {
        this.sourceDatabaseId = sourceDatabaseId;
    }

    public Long getTargetDatabaseId() {
        return targetDatabaseId;
    }

    public void setTargetDatabaseId(Long targetDatabaseId) {
        this.targetDatabaseId = targetDatabaseId;
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

    @NotNull(message = "Le statut est obligatoire")
    private NetworkStatus status;
}