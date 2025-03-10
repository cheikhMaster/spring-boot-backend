package com.project.memoireBackend.dto;

import com.project.memoireBackend.model.BackupType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BackupInitiateDTO {
    @NotNull(message = "L'ID de la base de données est obligatoire")
    private Long databaseId;

    @NotNull(message = "Le type de sauvegarde est obligatoire")
    private BackupType backupType;

    public Long getDatabaseId() {
        return databaseId;
    }

    public void setDatabaseId(Long databaseId) {
        this.databaseId = databaseId;
    }

    public BackupType getBackupType() {
        return backupType;
    }

    public void setBackupType(BackupType backupType) {
        this.backupType = backupType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    private String description;
}