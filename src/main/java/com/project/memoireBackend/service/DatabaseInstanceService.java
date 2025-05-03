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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
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
        return databaseInstanceRepository.findByLocal(true).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<DatabaseInstanceDTO> getRemoteDatabaseInstances() {
        return databaseInstanceRepository.findByLocal(false).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
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






    @Transactional
    public DatabaseInstanceDTO createDatabaseInstance(DatabaseInstanceCreateDTO instanceCreateDTO, String createdByUsername) {
        System.out.println("Création - isLocal reçu : " + instanceCreateDTO.isLocal());

        DatabaseInstance instance = new DatabaseInstance();
        instance.setName(instanceCreateDTO.getName());
        instance.setType(instanceCreateDTO.getType());
        instance.setAddress(instanceCreateDTO.getAddress());
        instance.setPort(instanceCreateDTO.getPort());
        instance.setUsername(instanceCreateDTO.getUsername());
        instance.setPassword(instanceCreateDTO.getPassword());
        instance.setStatus(DatabaseStatus.INACTIVE);
        instance.setLocal(Boolean.TRUE.equals(instanceCreateDTO.isLocal()));

        // Champs Oracle
        instance.setSid(instanceCreateDTO.getSid());
        instance.setServiceName(instanceCreateDTO.getServiceName());
        instance.setTnsName(instanceCreateDTO.getTnsName());
        instance.setTnsAdmin(instanceCreateDTO.getTnsAdmin());

        System.out.println("Création - isLocal après assignation : " + instance.isLocal());

        DatabaseInstance savedInstance = databaseInstanceRepository.save(instance);

        System.out.println("Création - isLocal après sauvegarde : " + savedInstance.isLocal());

        // Journalisation...
        activityLogService.logDatabaseActivity(
                createdByUsername,
                "ADD_DATABASE",
                "Ajout de la base de données: " + instance.getName() +
                        (instance.isLocal() ? " (locale)" : " (distante)"),
                savedInstance.getId()
        );

        return convertToDTO(savedInstance);
    }

    @Transactional
    public DatabaseInstanceDTO updateDatabaseInstance(Long id, DatabaseInstanceCreateDTO instanceDetails, String updatedByUsername) {
        DatabaseInstance instance = databaseInstanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Base de données non trouvée avec l'id: " + id));

        boolean oldLocalStatus = instance.isLocal();

        instance.setName(instanceDetails.getName());
        instance.setType(instanceDetails.getType());
        instance.setAddress(instanceDetails.getAddress());
        instance.setPort(instanceDetails.getPort());
        instance.setUsername(instanceDetails.getUsername());

        // Mettre à jour le mot de passe seulement s'il est fourni
        if (instanceDetails.getPassword() != null && !instanceDetails.getPassword().isEmpty()) {
            instance.setPassword(instanceDetails.getPassword());
        }

        instance.setLocal(instanceDetails.isLocal());

        // Champs Oracle
        instance.setSid(instanceDetails.getSid());
        instance.setServiceName(instanceDetails.getServiceName());
        instance.setTnsName(instanceDetails.getTnsName());
        instance.setTnsAdmin(instanceDetails.getTnsAdmin());

        DatabaseInstance updatedInstance = databaseInstanceRepository.save(instance);

        // Journalisation avec détail sur le changement de statut local/distant
        String details = "Mise à jour de la base de données: " + instance.getName();
        if (oldLocalStatus != instance.isLocal()) {
            details += " - Changement de statut: " +
                    (oldLocalStatus ? "locale" : "distante") +
                    " -> " +
                    (instance.isLocal() ? "locale" : "distante");
        }

        activityLogService.logDatabaseActivity(
                updatedByUsername,
                "MODIFY_DATABASE",
                details,
                updatedInstance.getId()
        );

        return convertToDTO(updatedInstance);
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

        System.out.println("ConvertToDTO - instance.isLocal() : " + instance.isLocal());
        dto.setLocal(instance.isLocal());
        System.out.println("ConvertToDTO - dto.isLocal() : " + dto.isLocal());

        // Champs Oracle
        dto.setSid(instance.getSid());
        dto.setServiceName(instance.getServiceName());
        dto.setTnsName(instance.getTnsName());
        dto.setTnsAdmin(instance.getTnsAdmin());

        return dto;
    }
    // Dans DatabaseInstanceService.java
    public boolean testConnection(DatabaseInstanceCreateDTO connectionDetails) {
        if (connectionDetails.getType() == DatabaseType.ORACLE) {
            return testOracleConnection(connectionDetails);
        } else if (connectionDetails.getType() == DatabaseType.MYSQL) {
            return testMySQLConnection(connectionDetails);
        } // etc. pour les autres types

        return false;
    }
    private boolean testMySQLConnection(DatabaseInstanceCreateDTO connectionDetails) {
        Connection connection = null;
        try {
            // Charger le driver MySQL
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Construire l'URL de connexion
            String url = "jdbc:mysql://" + connectionDetails.getAddress() + ":" +
                    connectionDetails.getPort() + "/?useSSL=false";

            // Tenter la connexion
            connection = DriverManager.getConnection(url, connectionDetails.getUsername(), connectionDetails.getPassword());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    private boolean testOracleConnection(DatabaseInstanceCreateDTO connectionDetails) {
        Connection connection = null;
        try {
            // Charger le driver Oracle
            Class.forName("oracle.jdbc.driver.OracleDriver");

            // Construire l'URL de connexion selon le type
            String url;
            if (connectionDetails.getSid() != null && !connectionDetails.getSid().isEmpty()) {
                // Connexion via SID
                url = "jdbc:oracle:thin:@" + connectionDetails.getAddress() + ":"
                        + connectionDetails.getPort() + ":" + connectionDetails.getSid();
            } else if (connectionDetails.getServiceName() != null && !connectionDetails.getServiceName().isEmpty()) {
                // Connexion via Service Name
                url = "jdbc:oracle:thin:@//" + connectionDetails.getAddress() + ":"
                        + connectionDetails.getPort() + "/" + connectionDetails.getServiceName();
            } else {
                // Connexion via TNS
                System.setProperty("oracle.net.tns_admin", connectionDetails.getTnsAdmin());
                url = "jdbc:oracle:thin:@" + connectionDetails.getTnsName();
            }

            // Tenter la connexion
            connection = DriverManager.getConnection(url, connectionDetails.getUsername(), connectionDetails.getPassword());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
