package com.project.memoireBackend.service;



import com.project.memoireBackend.dto.BackupDTO;
import com.project.memoireBackend.dto.BackupInitiateDTO;
import com.project.memoireBackend.excepton.ResourceNotFoundException;
import com.project.memoireBackend.model.*;
import com.project.memoireBackend.repository.BackupRepository;
import com.project.memoireBackend.repository.DatabaseInstanceRepository;
import com.project.memoireBackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class  BackupService {

    @Autowired
    private BackupRepository backupRepository;

    @Autowired
    private DatabaseInstanceRepository databaseInstanceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActivityLogService activityLogService;

    public List<BackupDTO> getAllBackups() {
        return backupRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public BackupDTO getBackupById(Long id) {
        Backup backup = backupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sauvegarde non trouvée avec l'id: " + id));
        return convertToDTO(backup);
    }

    public List<BackupDTO> getBackupsByDatabaseId(Long databaseId) {
        DatabaseInstance instance = databaseInstanceRepository.findById(databaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Base de données non trouvée avec l'id: " + databaseId));

        return backupRepository.findByDatabaseInstance(instance).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<BackupDTO> getBackupsByStatus(BackupStatus status) {
        return backupRepository.findByStatus(status).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<BackupDTO> getBackupsByType(BackupType type) {
        return backupRepository.findByBackupType(type).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<BackupDTO> getBackupsByDateRange(LocalDateTime start, LocalDateTime end) {
        return backupRepository.findByStartTimeBetween(start, end).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }


    @Transactional
    public BackupDTO initiateBackup(BackupInitiateDTO backupInitiateDTO, String username) {
        DatabaseInstance instance = databaseInstanceRepository.findById(backupInitiateDTO.getDatabaseId())
                .orElseThrow(() -> new ResourceNotFoundException("Base de données non trouvée avec l'id: " + backupInitiateDTO.getDatabaseId()));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec le nom: " + username));

        Backup backup = new Backup();
        backup.setDatabaseInstance(instance);
        backup.setStartTime(LocalDateTime.now());
        backup.setStatus(BackupStatus.PENDING);
        backup.setBackupType(backupInitiateDTO.getBackupType());
        backup.setCreatedBy(user);

        Backup savedBackup = backupRepository.save(backup);

        // Journalisation
        activityLogService.logBackupActivity(
                username,
                "START_BACKUP",
                "Démarrage d'une sauvegarde " + backup.getBackupType() + " pour la base: " + instance.getName(),
                instance.getId(),
                savedBackup.getId()
        );

        // Lancer le processus de sauvegarde de manière asynchrone
        startBackupProcess(savedBackup.getId(), username);

        return convertToDTO(savedBackup);
    }

    @Async
    public CompletableFuture<Void> startBackupProcess(Long backupId, String username) {
        return CompletableFuture.runAsync(() -> {
            try {
                Backup backup = backupRepository.findById(backupId)
                        .orElseThrow(() -> new ResourceNotFoundException("Sauvegarde non trouvée avec l'id: " + backupId));

                DatabaseInstance instance = backup.getDatabaseInstance();

                // Mettre à jour le statut
                backup.setStatus(BackupStatus.IN_PROGRESS);
                backupRepository.save(backup);

                // Logique de sauvegarde réelle ici
                // En fonction du type de base de données
                boolean success = performBackup(instance, backup);

                // Mise à jour du statut final
                backup.setEndTime(LocalDateTime.now());
                if (success) {
                    backup.setStatus(BackupStatus.COMPLETED);

                    // Journalisation succès
                    activityLogService.logBackupActivity(
                            username,
                            "COMPLETE_BACKUP",
                            "Sauvegarde terminée avec succès pour la base: " + instance.getName(),
                            instance.getId(),
                            backup.getId()
                    );
                } else {
                    backup.setStatus(BackupStatus.FAILED);
                    backup.setErrorMessage("Erreur lors du processus de sauvegarde");

                    // Journalisation échec
                    activityLogService.logBackupActivity(
                            username,
                            "FAILED_BACKUP",
                            "Échec de la sauvegarde pour la base: " + instance.getName(),
                            instance.getId(),
                            backup.getId()
                    );
                }

                backupRepository.save(backup);

            } catch (Exception e) {
                // Gérer les exceptions
                Backup backup = backupRepository.findById(backupId).orElse(null);
                if (backup != null) {
                    DatabaseInstance instance = backup.getDatabaseInstance();

                    backup.setStatus(BackupStatus.FAILED);
                    backup.setErrorMessage(e.getMessage());
                    backup.setEndTime(LocalDateTime.now());
                    backupRepository.save(backup);

                    // Journalisation exception
                    activityLogService.logBackupActivity(
                            username,
                            "FAILED_BACKUP",
                            "Exception lors de la sauvegarde de la base: " + instance.getName() + ". Erreur: " + e.getMessage(),
                            instance.getId(),
                            backup.getId()
                    );
                }
            }
        });
    }

    private boolean performBackup(DatabaseInstance instance, Backup backup) {
        // Logique de sauvegarde à implémenter en fonction du type de base de données
        try {
            // Simulation d'un processus de sauvegarde
            // Dans une implémentation réelle, la logique dépendrait du type de base de données
            Thread.sleep(5000); // Simule un travail de 5 secondes

            // Définir le chemin du fichier de sauvegarde
            String filePath = "/backups/" + instance.getName() + "_"
                    + LocalDateTime.now().toString().replace(":", "-") + ".bak";
            backup.setFilePath(filePath);
            backup.setFileSize(1024L * 1024L); // Exemple: 1 MB

            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Transactional
    public BackupDTO cancelBackup(Long id, String username) {
        Backup backup = backupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sauvegarde non trouvée avec l'id: " + id));

        // On ne peut annuler que les sauvegardes en attente ou en cours
        if (backup.getStatus() == BackupStatus.PENDING || backup.getStatus() == BackupStatus.IN_PROGRESS) {
            backup.setStatus(BackupStatus.CANCELED);
            backup.setEndTime(LocalDateTime.now());

            Backup canceledBackup = backupRepository.save(backup);

            // Journalisation
            activityLogService.logBackupActivity(
                    username,
                    "CANCEL_BACKUP",
                    "Annulation de la sauvegarde pour la base: " + backup.getDatabaseInstance().getName(),
                    backup.getDatabaseInstance().getId(),
                    backup.getId()
            );

            return convertToDTO(canceledBackup);
        } else {
            throw new IllegalStateException("Impossible d'annuler une sauvegarde qui n'est pas en attente ou en cours");
        }
    }

    @Transactional
    public void deleteBackup(Long id, String username) {
        Backup backup = backupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sauvegarde non trouvée avec l'id: " + id));

        Long databaseId = backup.getDatabaseInstance().getId();
        String databaseName = backup.getDatabaseInstance().getName();

        backupRepository.delete(backup);

        // Journalisation
        activityLogService.logBackupActivity(
                username,
                "DELETE_BACKUP",
                "Suppression de la sauvegarde pour la base: " + databaseName,
                databaseId,
                null
        );
    }

    private BackupDTO convertToDTO(Backup backup) {
        BackupDTO dto = new BackupDTO();
        dto.setId(backup.getId());
        dto.setDatabaseId(backup.getDatabaseInstance().getId());
        dto.setDatabaseName(backup.getDatabaseInstance().getName());
        dto.setStartTime(backup.getStartTime());
        dto.setEndTime(backup.getEndTime());
        dto.setStatus(backup.getStatus());
        dto.setFilePath(backup.getFilePath());
        dto.setBackupType(backup.getBackupType());
        dto.setFileSize(backup.getFileSize());
        dto.setErrorMessage(backup.getErrorMessage());

        if (backup.getCreatedBy() != null) {
            dto.setCreatedByUsername(backup.getCreatedBy().getUsername());
        }

        return dto;
    }
}