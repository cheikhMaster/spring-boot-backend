package com.project.memoireBackend.controller;



import com.project.memoireBackend.dto.DatabaseInstanceCreateDTO;
import com.project.memoireBackend.dto.DatabaseInstanceDTO;
import com.project.memoireBackend.model.DatabaseStatus;
import com.project.memoireBackend.model.DatabaseType;
import com.project.memoireBackend.service.DatabaseInstanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/databases")
public class DatabaseInstanceController {

    @Autowired
    private DatabaseInstanceService databaseInstanceService;

    @GetMapping
    public ResponseEntity<List<DatabaseInstanceDTO>> getAllDatabases() {
        return ResponseEntity.ok(databaseInstanceService.getAllDatabaseInstances());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DatabaseInstanceDTO> getDatabaseById(@PathVariable Long id) {
        return ResponseEntity.ok(databaseInstanceService.getDatabaseInstanceById(id));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<DatabaseInstanceDTO>> getDatabasesByStatus(@PathVariable DatabaseStatus status) {
        return ResponseEntity.ok(databaseInstanceService.getDatabaseInstancesByStatus(status));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<DatabaseInstanceDTO>> getDatabasesByType(@PathVariable DatabaseType type) {
        return ResponseEntity.ok(databaseInstanceService.getDatabaseInstancesByType(type));
    }

    @GetMapping("/local")
    public ResponseEntity<List<DatabaseInstanceDTO>> getLocalDatabases() {
        return ResponseEntity.ok(databaseInstanceService.getLocalDatabaseInstances());
    }

    @GetMapping("/remote")
    public ResponseEntity<List<DatabaseInstanceDTO>> getRemoteDatabases() {
        return ResponseEntity.ok(databaseInstanceService.getRemoteDatabaseInstances());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DatabaseInstanceDTO> createDatabase(@Validated @RequestBody DatabaseInstanceCreateDTO databaseCreateDTO) {
        System.out.println("Contrôleur - JSON reçu : " + databaseCreateDTO.isLocal());
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        return ResponseEntity.ok(databaseInstanceService.createDatabaseInstance(databaseCreateDTO, currentUsername));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DatabaseInstanceDTO> updateDatabase(@PathVariable Long id, @Validated @RequestBody DatabaseInstanceCreateDTO databaseDetails) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        return ResponseEntity.ok(databaseInstanceService.updateDatabaseInstance(id, databaseDetails, currentUsername));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DatabaseInstanceDTO> updateDatabaseStatus(@PathVariable Long id, @RequestParam DatabaseStatus status) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        return ResponseEntity.ok(databaseInstanceService.updateDatabaseStatus(id, status, currentUsername));
    }




    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteDatabase(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        databaseInstanceService.deleteDatabaseInstance(id, currentUsername);
        return ResponseEntity.ok().build();
    }
    // Dans DatabaseInstanceController.java
    @PostMapping("/test-connection")
    public ResponseEntity<Map<String, Object>> testDatabaseConnection(@RequestBody DatabaseInstanceCreateDTO connectionDetails) {
        try {
            boolean success = databaseInstanceService.testConnection(connectionDetails);
            Map<String, Object> response = new HashMap<>();

            if (success) {
                response.put("success", true);
                response.put("message", "Connexion réussie");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Échec de la connexion");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Erreur: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
}
