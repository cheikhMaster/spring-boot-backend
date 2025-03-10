package com.project.memoireBackend.model;

import com.project.memoireBackend.dto.DatabaseMetric;
import jakarta.persistence.*;
import lombok.Data;
import java.util.Set;

@Entity
@Table(name = "database_instances")
@Data
public class DatabaseInstance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DatabaseType type;

    @Column(nullable = false)
    private String address;

    private int port;
    private String username;
    private String password; // À encoder

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DatabaseType getType() {
        return type;
    }

    public void setType(DatabaseType type) {
        this.type = type;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public DatabaseStatus getStatus() {
        return status;
    }

    public void setStatus(DatabaseStatus status) {
        this.status = status;
    }

    public boolean isLocal() {
        return isLocal;
    }

    public void setLocal(boolean local) {
        isLocal = local;
    }

    public Set<Backup> getBackups() {
        return backups;
    }

    public void setBackups(Set<Backup> backups) {
        this.backups = backups;
    }

    public Set<DatabaseMetric> getMetrics() {
        return metrics;
    }

    public void setMetrics(Set<DatabaseMetric> metrics) {
        this.metrics = metrics;
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DatabaseStatus status;

    private boolean isLocal;

    @OneToMany(mappedBy = "databaseInstance", cascade = CascadeType.ALL)
    private Set<Backup> backups;

    @OneToMany(mappedBy = "databaseInstance", cascade = CascadeType.ALL)
    private Set<DatabaseMetric> metrics;
}

