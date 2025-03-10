package com.project.memoireBackend.service;


import com.project.memoireBackend.dto.DatabaseInstanceCreateDTO;
import com.project.memoireBackend.dto.DatabaseInstanceDTO;
import com.project.memoireBackend.excepton.ResourceNotFoundException;
import com.project.memoireBackend.model.DatabaseInstance;
import com.project.memoireBackend.model.DatabaseStatus;
import com.project.memoireBackend.model.DatabaseType;
import com.project.memoireBackend.repository.DatabaseInstanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DatabaseInstanceService {

    @Autowired
    private DatabaseInstanceRepository databaseInstanceRepository;

    @Autowired
    private ActivityLogService activityLogService;

    public List<DatabaseInstanceDTO> getAllDatabaseInstances() {
        return databaseInstanceRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public DatabaseInstanceDTO getDatabaseInstanceById(Long id) {
        DatabaseInstance instance = databaseInstanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Base de données non trouvée avec l'id: " + id));
        return convertToDTO(instance);
    }

    public List<DatabaseInstanceDTO> getDatabaseInstancesByStatus(DatabaseStatus status) {
        return databaseInstanceRepository.findByStatus(status).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<DatabaseInstanceDTO> getDatabaseInstancesByType(DatabaseType type) {
        return databaseInstanceRepository.findByType(type).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<DatabaseInstanceDTO> getLocalDatabaseInstances() {
        return databaseInstanceRepository.findByIsLocal(true).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<DatabaseInstanceDTO> getRemoteDatabaseInstances() {
        return databaseInstanceRepository.findByIsLocal(false).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public DatabaseInstanceDTO createDatabaseInstance(DatabaseInstanceCreateDTO instanceCreateDTO, String createdByUsername) {
        DatabaseInstance instance = new DatabaseInstance();
        instance.setName(instanceCreateDTO.getName());
        instance.setType(instanceCreateDTO.getType());
        instance.setAddress(instanceCreateDTO.getAddress());
        instance.setPort(instanceCreateDTO.getPort());
        instance.setUsername(instanceCreateDTO.getUsername());
        instance.setPassword(instanceCreateDTO.getPassword()); // À encoder dans une implémentation réelle
        instance.setStatus(DatabaseStatus.INACTIVE); // Par défaut, une nouvelle base est inactive
        instance.setLocal(instanceCreateDTO.isLocal());

        DatabaseInstance savedInstance = databaseInstanceRepository.save(instance);

        // Journalisation
        activityLogService.logDatabaseActivity(
                createdByUsername,
                "ADD_DATABASE",
                "Ajout de la base de données: " + instance.getName(),
                savedInstance.getId()
        );

        return convertToDTO(savedInstance);
    }

    @Transactional
    public DatabaseInstanceDTO updateDatabaseInstance(Long id, DatabaseInstanceCreateDTO instanceDetails, String updatedByUsername) {
        DatabaseInstance instance = databaseInstanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Base de données non trouvée avec l'id: " + id));

        instance.setName(instanceDetails.getName());
        instance.setType(instanceDetails.getType());
        instance.setAddress(instanceDetails.getAddress());
        instance.setPort(instanceDetails.getPort());
        instance.setUsername(instanceDetails.getUsername());

        // Mettre à jour le mot de passe seulement s'il est fourni
        if (instanceDetails.getPassword() != null && !instanceDetails.getPassword().isEmpty()) {
            instance.setPassword(instanceDetails.getPassword()); // À encoder dans une implémentation réelle
        }

        instance.setLocal(instanceDetails.isLocal());

        DatabaseInstance updatedInstance = databaseInstanceRepository.save(instance);

        // Journalisation
        activityLogService.logDatabaseActivity(
                updatedByUsername,
                "MODIFY_DATABASE",
                "Mise à jour de la base de données: " + instance.getName(),
                updatedInstance.getId()
        );

        return convertToDTO(updatedInstance);
    }

    @Transactional
    public DatabaseInstanceDTO updateDatabaseStatus(Long id, DatabaseStatus status, String updatedByUsername) {
        DatabaseInstance instance = databaseInstanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Base de données non trouvée avec l'id: " + id));

        DatabaseStatus oldStatus = instance.getStatus();
        instance.setStatus(status);

        DatabaseInstance updatedInstance = databaseInstanceRepository.save(instance);

        // Journalisation
        activityLogService.logDatabaseActivity(
                updatedByUsername,
                "UPDATE_DATABASE_STATUS",
                "Changement de statut de la base de données: " + instance.getName() +
                        " de " + oldStatus + " à " + status,
                updatedInstance.getId()
        );

        return convertToDTO(updatedInstance);
    }

    @Transactional
    public void deleteDatabaseInstance(Long id, String deletedByUsername) {
        DatabaseInstance instance = databaseInstanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Base de données non trouvée avec l'id: " + id));

        String name = instance.getName();
        databaseInstanceRepository.delete(instance);

        // Journalisation
        activityLogService.logUserActivity(
                deletedByUsername,
                "DELETE_DATABASE",
                "Suppression de la base de données: " + name
        );
    }

    private DatabaseInstanceDTO convertToDTO(DatabaseInstance instance) {
        DatabaseInstanceDTO dto = new DatabaseInstanceDTO();
        dto.setId(instance.getId());
        dto.setName(instance.getName());
        dto.setType(instance.getType());
        dto.setAddress(instance.getAddress());
        dto.setPort(instance.getPort());
        dto.setUsername(instance.getUsername());
        dto.setStatus(instance.getStatus());
        dto.setLocal(instance.isLocal());
        return dto;
    }
}
