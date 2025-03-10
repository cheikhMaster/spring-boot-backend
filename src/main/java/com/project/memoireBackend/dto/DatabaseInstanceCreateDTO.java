package com.project.memoireBackend.dto;

import com.project.memoireBackend.model.DatabaseType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
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

    public boolean isLocal() {
        return isLocal;
    }

    public void setLocal(boolean local) {
        isLocal = local;
    }

    private String username;
    private String password;
    private boolean isLocal;
}