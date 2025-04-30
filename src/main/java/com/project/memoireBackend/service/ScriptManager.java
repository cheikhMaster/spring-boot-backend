package com.project.memoireBackend.service;



import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;

@Service
public class ScriptManager {

    private static final Logger logger = LoggerFactory.getLogger(ScriptManager.class);

    @Value("${app.scripts.directory:/tmp/dbbackup/scripts}")
    private String scriptsDirectory;

    @PostConstruct
    public void init() {
        try {
            // Créer le répertoire des scripts s'il n'existe pas
            Path scriptsDirPath = Paths.get(scriptsDirectory);
            if (!Files.exists(scriptsDirPath)) {
                Files.createDirectories(scriptsDirPath);
                logger.info("Répertoire de scripts créé: {}", scriptsDirectory);
            }

            // Extraire et copier le script oracle_hot_backup.sh
            extractScript("scripts/oracle_hot_backup.sh", "oracle_hot_backup.sh");

        } catch (IOException e) {
            logger.error("Erreur lors de l'initialisation des scripts", e);
        }
    }

    /**
     * Extrait un script des ressources de l'application vers le répertoire d'exécution
     *
     * @param resourcePath Chemin du script dans les ressources
     * @param scriptName Nom du script à extraire
     * @return Chemin complet vers le script extrait
     * @throws IOException En cas d'erreur d'extraction
     */
    private String extractScript(String resourcePath, String scriptName) throws IOException {
        ClassPathResource resource = new ClassPathResource(resourcePath);
        Path destination = Paths.get(scriptsDirectory, scriptName);

        // Copier le script depuis les ressources vers le répertoire d'exécution
        Files.copy(resource.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        logger.info("Script extrait: {}", destination);

        // Rendre le script exécutable (pour Unix/Linux)
        File scriptFile = destination.toFile();
        makeExecutable(scriptFile);

        return destination.toString();
    }

    /**
     * Rend un fichier exécutable
     *
     * @param file Fichier à rendre exécutable
     */
    private void makeExecutable(File file) {
        if (file.exists()) {
            try {
                // Pour les systèmes de type Unix/Linux
                if (!System.getProperty("os.name").toLowerCase().contains("windows")) {
                    Set<PosixFilePermission> perms = new HashSet<>();
                    perms.add(PosixFilePermission.OWNER_READ);
                    perms.add(PosixFilePermission.OWNER_WRITE);
                    perms.add(PosixFilePermission.OWNER_EXECUTE);
                    perms.add(PosixFilePermission.GROUP_READ);
                    perms.add(PosixFilePermission.GROUP_EXECUTE);

                    Files.setPosixFilePermissions(file.toPath(), perms);
                } else {
                    // Pour Windows
                    file.setExecutable(true, false);
                }
                logger.info("Script rendu exécutable: {}", file.getAbsolutePath());
            } catch (IOException e) {
                logger.error("Erreur lors du changement des permissions", e);
            }
        }
    }

    /**
     * Obtient le chemin complet d'un script
     *
     * @param scriptName Nom du script
     * @return Chemin complet du script
     */
    public String getScriptPath(String scriptName) {
        return Paths.get(scriptsDirectory, scriptName).toString();
    }
}