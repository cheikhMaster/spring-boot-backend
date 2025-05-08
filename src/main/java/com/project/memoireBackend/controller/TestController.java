package com.project.memoireBackend.controller;

import com.project.memoireBackend.excepton.ResourceNotFoundException;
import com.project.memoireBackend.model.DatabaseInstance;
import com.project.memoireBackend.repository.DatabaseInstanceRepository;
import com.project.memoireBackend.service.SshService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @Autowired
    private SshService sshService;

    @Autowired
    private DatabaseInstanceRepository databaseInstanceRepository;

    @GetMapping("/ssh/{id}")
    public ResponseEntity<Map<String, Object>> testSshConnection(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();

        try {
            DatabaseInstance instance = databaseInstanceRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Base de données non trouvée"));

            String host = instance.getAddress();
            int port = 22; // Utilisez le port SSH standard
            String username = instance.getUsername();
            String password = instance.getPassword();

            boolean success = sshService.testConnection(host, port, username, password);

            response.put("success", success);
            response.put("message", success ?
                    "Connexion SSH réussie" :
                    "Échec de la connexion SSH");
            response.put("config", Map.of(
                    "host", host,
                    "port", port,
                    "username", username
            ));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Erreur: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/ssh")
    public ResponseEntity<Map<String, Object>> testManualSshConnection(
            @RequestBody Map<String, Object> params) {
        Map<String, Object> response = new HashMap<>();

        try {
            String host = (String) params.get("host");
            Integer port = params.containsKey("port") ?
                    Integer.parseInt(params.get("port").toString()) : 22;
            String username = (String) params.get("username");
            String password = (String) params.get("password");

            // Validation des paramètres
            if (host == null || username == null || password == null) {
                response.put("success", false);
                response.put("message", "Paramètres manquants");
                return ResponseEntity.badRequest().body(response);
            }

            boolean success = sshService.testConnection(host, port, username, password);

            response.put("success", success);
            response.put("message", success ?
                    "Connexion SSH réussie" :
                    "Échec de la connexion SSH");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Erreur: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}