package com.project.memoireBackend.config;

import com.project.memoireBackend.model.User;
import com.project.memoireBackend.model.UserRole;
import com.project.memoireBackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Configuration
public class InitialDataConfig {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.scripts.directory:C:/Users/Cheikhna/oracle-scripts}")
    private String scriptsDirectory;

    @Bean
    public CommandLineRunner initializeDefaultUsers() {
        return args -> {
            // Vérifier et créer un compte administrateur
            if (!userRepository.existsByUsername("admin")) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setEmail("admin@gmail.com");
                admin.setRole(UserRole.ADMIN);
                admin.setActive(true);
                userRepository.save(admin);
                System.out.println("Utilisateur admin créé avec succès");
            } else {
                System.out.println("Utilisateur admin existe déjà");
            }

            // Vérifier et créer un compte utilisateur standard
            if (!userRepository.existsByUsername("user")) {
                User user = new User();
                user.setUsername("user");
                user.setPassword(passwordEncoder.encode("user123"));
                user.setEmail("user@gmail.com");
                user.setRole(UserRole.USER);
                user.setActive(true);
                userRepository.save(user);
                System.out.println("Utilisateur standard créé avec succès");
            } else {
                System.out.println("Utilisateur standard existe déjà");
            }
        };
    }

    @Bean
    public CommandLineRunner initializeOracleScripts() {
        return args -> {
            try {
                // Créer le répertoire des scripts s'il n'existe pas
                File scriptsDir = new File(scriptsDirectory);
                if (!scriptsDir.exists()) {
                    scriptsDir.mkdirs();
                    System.out.println("Répertoire des scripts créé: " + scriptsDirectory);
                }

                // Copier le script de sauvegarde Oracle local
                File localBackupScript = new File(scriptsDir, "oracle_hot_backup.bat");
                if (!localBackupScript.exists()) {
                    try (InputStream is = getClass().getResourceAsStream("/scripts/oracle_hot_backup.bat")) {
                        if (is != null) {
                            Files.copy(is, localBackupScript.toPath(), StandardCopyOption.REPLACE_EXISTING);
                            System.out.println("Script de sauvegarde Oracle local copié: " + localBackupScript.getAbsolutePath());
                        } else {
                            System.err.println("Script de sauvegarde Oracle local non trouvé dans les ressources!");
                        }
                    }
                }

                // Copier le script de sauvegarde Oracle distant
                File remoteBackupScript = new File(scriptsDir, "oracle_remote_backup.bat");
                if (!remoteBackupScript.exists()) {
                    try (InputStream is = getClass().getResourceAsStream("/scripts/oracle_remote_backup.bat")) {
                        if (is != null) {
                            Files.copy(is, remoteBackupScript.toPath(), StandardCopyOption.REPLACE_EXISTING);
                            System.out.println("Script de sauvegarde Oracle distant copié: " + remoteBackupScript.getAbsolutePath());
                        } else {
                            System.err.println("Script de sauvegarde Oracle distant non trouvé dans les ressources!");
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Erreur lors de l'initialisation des scripts Oracle: " + e.getMessage());
                e.printStackTrace();
            }
        };
    }
}