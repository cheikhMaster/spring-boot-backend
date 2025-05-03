package com.project.memoireBackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.project.memoireBackend.model.DatabaseType;



import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class DatabaseInstanceCreateDTO {
    @NotBlank(message = "Le nom est obligatoire")
    private String name;

    @NotNull(message = "Le type est obligatoire")
    private DatabaseType type;

    @NotBlank(message = "L'adresse est obligatoire")
    private String address;

    @Min(value = 1, message = "Le port doit être supérieur à 0")
    @Max(value = 65535, message = "Le port doit être inférieur à 65536")
    private int port;

    private String username;
    private String password;

    @JsonProperty("isLocal")
    private boolean local;

    // Champs spécifiques à Oracle
    private String sid;
    private String serviceName;
    private String tnsName;
    private String tnsAdmin;

    // Getter et Setter explicites pour isLocal
    public boolean isLocal() {
        return local;
    }

    public void setLocal(boolean local) {
        this.local = local;
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

    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
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
// Autres getters et setters...
}