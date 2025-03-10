package com.project.memoireBackend.model;



import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "network_connections")
@Data
public class NetworkConnection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "source_db_id", nullable = false)
    private DatabaseInstance sourceDatabase;

    @ManyToOne
    @JoinColumn(name = "target_db_id", nullable = false)
    private DatabaseInstance targetDatabase;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    private Double latency; // en millisecondes
    private Double throughput; // en Mbps
    private Long packetLoss; // en nombre de paquets

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NetworkStatus status;
}