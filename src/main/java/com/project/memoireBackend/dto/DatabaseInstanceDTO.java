package com.project.memoireBackend.dto;

import com.project.memoireBackend.model.DatabaseStatus;
import com.project.memoireBackend.model.DatabaseType;


public class DatabaseInstanceDTO {
    private Long id;
    private String name;
    private DatabaseType type;
    private String address;
    private int port;
    private String username;
    // Ne pas inclure le mot de passe dans le DTO pour des raisons de sécurité
    private DatabaseStatus status;
    private boolean isLocal;
    private String sid;        // SID Oracle
    private String serviceName; // Service Name Oracle
    private String tnsName;     // TNS Name pour les connexions via tnsnames.ora
    private String tnsAdmin;    // Chemin vers le répertoire contenant tnsnames.ora

    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    public String getTnsName() {
        return tnsName;
    }

    public void setTnsName(String tnsName) {
        this.tnsName = tnsName;
    }

    public String getTnsAdmin() {
        return tnsAdmin;
    }

    public void setTnsAdmin(String tnsAdmin) {
        this.tnsAdmin = tnsAdmin;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

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
}