package com.project.memoireBackend.service;

import com.jcraft.jsch.*;
import com.project.memoireBackend.model.Backup;
import com.project.memoireBackend.model.DatabaseInstance;
import com.project.memoireBackend.model.DatabaseType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class OracleBackupService {

    @Value("${app.scripts.directory:C:/path/to/scripts}")
    private String scriptsDirectory;

    @Value("${app.backup.output.directory:C:/Users/Cheikhna/backups}")
    private String backupOutputDirectory;

    @Autowired
    private SshService sshService;

    public boolean performHotBackup(DatabaseInstance instance, Backup backup) {
        try {
            // Vérifier que la base de données est de type Oracle
            if (instance.getType() != DatabaseType.ORACLE) {
                backup.setErrorMessage("Type de base de données non supporté: " + instance.getType());
                return false;
            }

            // Vérifier que le SID est défini
            if (instance.getSid() == null || instance.getSid().trim().isEmpty()) {
                backup.setErrorMessage("Le SID Oracle est requis pour effectuer une sauvegarde");
                return false;
            }

            // Créer un sous-répertoire spécifique pour cette sauvegarde
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
            String timestamp = LocalDateTime.now().format(formatter);
            String backupDir = backupOutputDirectory + "/oracle_backup_" + instance.getSid() + "_" + timestamp;

            boolean success;

            // Vérifier si la base de données est locale ou distante
            if (instance.isLocal()) {
                success = executeLocalBackup(instance, backup, backupDir);
            } else {
                success = executeRemoteBackup(instance, backup, backupDir);
            }

            if (success) {
                // Mettre à jour les informations de sauvegarde
                updateBackupInfo(backup, backupDir);
            }

            return success;
        } catch (Exception e) {
            backup.setErrorMessage("Erreur lors de la sauvegarde Oracle: " + e.getMessage());
            return false;
        }
    }

    private boolean executeLocalBackup(DatabaseInstance instance, Backup backup, String backupDir) {
        try {
            // Préparer le chemin du script
            String scriptPath = scriptsDirectory + "/oracle_hot_backup.bat";
            File scriptFile = new File(scriptPath);

            System.out.println("Vérification du script: " + scriptPath);

            // Vérifier que le script existe et est exécutable
            if (!scriptFile.exists()) {
                String error = "Le script de sauvegarde n'existe pas: " + scriptPath;
                System.out.println(error);
                backup.setErrorMessage(error);
                return false;
            }

            // S'assurer que le répertoire de destination existe
            System.out.println("Création du répertoire de sauvegarde: " + backupDir);
            File backupDirFile = new File(backupDir);
            backupDirFile.mkdirs();

            // Convertir le type de sauvegarde en format standardisé
            String backupTypeParam = backup.getBackupType().toString();
            // Gérer à la fois les versions anglaises et françaises
            if (backupTypeParam.equalsIgnoreCase("FULL") ||
                    backupTypeParam.equalsIgnoreCase("Complète") ||
                    backupTypeParam.equalsIgnoreCase("Complete")) {
                backupTypeParam = "FULL";
            } else if (backupTypeParam.equalsIgnoreCase("INCREMENTAL") ||
                    backupTypeParam.equalsIgnoreCase("Incrémentale") ||
                    backupTypeParam.equalsIgnoreCase("Incrementale")) {
                backupTypeParam = "INCREMENTAL";
            }

            System.out.println("Type de sauvegarde normalisé: " + backupTypeParam);

            // Préparer la commande
            ProcessBuilder pb = new ProcessBuilder(
                    "cmd.exe", "/c",
                    scriptPath,
                    instance.getSid(),
                    backupTypeParam,
                    backupDir
            );

            // Configurer l'environnement Oracle pour 10g
            pb.environment().put("ORACLE_HOME", "C:\\oracle\\product\\10.2.0\\db_1");
            pb.environment().put("ORACLE_SID", instance.getSid());

            // Rediriger la sortie standard et d'erreur
            pb.redirectErrorStream(true);

            // Exécuter le processus
            System.out.println("Démarrage du processus de sauvegarde");
            Process process = pb.start();

            // Lire la sortie du processus
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    System.out.println("Script > " + line);
                }
            }

            // Attendre la fin du processus
            System.out.println("Attente de la fin du processus");
            int exitCode = process.waitFor();
            System.out.println("Processus terminé avec le code: " + exitCode);

            if (exitCode == 0) {
                System.out.println("Sauvegarde réussie");
                return true;
            } else {
                String error = "Échec de la sauvegarde Oracle avec le code: " + exitCode + "\n" + output.toString();
                System.out.println(error);
                backup.setErrorMessage(error);
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            String error = "Erreur lors de l'exécution de la sauvegarde locale: " + e.getMessage();
            System.out.println(error);
            backup.setErrorMessage(error);
            return false;
        }
    }

    private boolean executeRemoteBackup(DatabaseInstance instance, Backup backup, String backupDir) {
        try {
            // 1. Créer le répertoire local pour stocker la sauvegarde finale
            System.out.println("Création du répertoire local de sauvegarde: " + backupDir);
            Files.createDirectories(Paths.get(backupDir));

            // 2. Paramètres de connexion
            String remoteHost = instance.getAddress();
            int remotePort = 22; // Port SSH standard
            String remoteUser = instance.getUsername();
            String remotePassword = instance.getPassword();
            String remoteSid = instance.getSid();

            System.out.println("Tentative de sauvegarde Oracle 10g distante pour " + instance.getName() +
                    " (" + remoteSid + ") sur " + remoteHost);

            // Test préliminaire de la connexion SSH
            System.out.println("Test préliminaire de la connexion SSH...");
            boolean sshTestSuccess = sshService.testConnection(remoteHost, remotePort, remoteUser, remotePassword);

            if (!sshTestSuccess) {
                String error = "Échec du test de connexion SSH préliminaire";
                System.out.println(error);
                backup.setErrorMessage(error);
                return false;
            }

            // 3. Chemin spécifique du script de sauvegarde sur la machine distante
            String remoteScriptPath = "C:\\Users\\cheikhna\\oracle-scripts\\oracle_remote_backup.bat";

            // 4. Définir un répertoire temporaire sur la machine distante pour stocker les fichiers de sauvegarde
            String timestamp = System.currentTimeMillis() + "";
            String remoteTempDir = "C:\\Temp\\backup_" + remoteSid + "_" + timestamp;

            // 5. Créer un répertoire temporaire sur la machine distante
            boolean mkdirSuccess = sshService.executeCommand(
                    remoteHost, remotePort, remoteUser, remotePassword,
                    "cmd.exe /c mkdir \"" + remoteTempDir + "\""
            );

            if (!mkdirSuccess) {
                String error = "Échec de la création du répertoire temporaire distant";
                System.out.println(error);
                backup.setErrorMessage(error);
                return false;
            }

            // 6. Vérifier l'existence du script de sauvegarde
            boolean scriptExists = sshService.executeCommand(
                    remoteHost, remotePort, remoteUser, remotePassword,
                    "cmd.exe /c if exist \"" + remoteScriptPath + "\" (exit 0) else (exit 1)"
            );

            if (!scriptExists) {
                String error = "Le script de sauvegarde n'existe pas sur le serveur distant: " + remoteScriptPath;
                System.out.println(error);
                backup.setErrorMessage(error);
                return false;
            }

            // 7. Type de sauvegarde normalisé
            String backupType = normalizeBackupType(backup.getBackupType().toString());

            // 8. Exécuter le script de sauvegarde avec les paramètres appropriés
            String execCommand = "cmd.exe /c \"\"" + remoteScriptPath + "\" " +
                    remoteSid + " " +
                    backupType + " \"" +
                    remoteTempDir + "\"\"";

            System.out.println("Exécution de la commande de sauvegarde: " + execCommand);
            boolean backupSuccess = sshService.executeCommandWithOutput(
                    remoteHost, remotePort, remoteUser, remotePassword,
                    execCommand,
                    (output, error) -> {
                        // Logger la sortie standard et d'erreur du script
                        System.out.println("Sortie standard: " + output);
                        if (!error.isEmpty()) {
                            System.err.println("Erreur: " + error);
                            if (backup.getErrorMessage() == null) {
                                backup.setErrorMessage("Erreurs pendant l'exécution: " + error);
                            }
                        }
                    }
            );

            if (!backupSuccess) {
                String error = "Échec de l'exécution du script de sauvegarde distant";
                System.out.println(error);
                if (backup.getErrorMessage() == null) {
                    backup.setErrorMessage(error);
                }
                return false;
            }

            // 9. Copier les fichiers de sauvegarde depuis le serveur distant
            System.out.println("Copie des fichiers de sauvegarde vers le serveur local...");
            boolean copySuccess = sshService.copyRemoteDirectory(
                    remoteHost, remotePort, remoteUser, remotePassword,
                    remoteTempDir, backupDir
            );

            if (!copySuccess) {
                String error = "Échec de la copie des fichiers de sauvegarde";
                System.out.println(error);
                backup.setErrorMessage(error);
                return false;
            }

            // 10. Nettoyage : supprimer le répertoire temporaire distant
            boolean cleanupSuccess = sshService.executeCommand(
                    remoteHost, remotePort, remoteUser, remotePassword,
                    "cmd.exe /c rmdir /S /Q \"" + remoteTempDir + "\""
            );

            if (!cleanupSuccess) {
                System.out.println("Attention: Échec du nettoyage du répertoire temporaire distant");
                // Ne pas échouer la sauvegarde pour un simple échec de nettoyage
            }

            System.out.println("Sauvegarde Oracle 10g à distance terminée avec succès");
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            String error = "Erreur lors de l'exécution de la sauvegarde distante: " + e.getMessage();
            System.out.println(error);
            backup.setErrorMessage(error);
            return false;
        }
    }




    private String normalizeBackupType(String backupType) {
        if (backupType.equalsIgnoreCase("FULL") ||
                backupType.equalsIgnoreCase("Complète") ||
                backupType.equalsIgnoreCase("Complete")) {
            return "FULL";
        } else if (backupType.equalsIgnoreCase("INCREMENTAL") ||
                backupType.equalsIgnoreCase("Incrémentale") ||
                backupType.equalsIgnoreCase("Incrementale")) {
            return "INCREMENTAL";
        }
        return backupType;
    }

    // Méthodes d'aide pour exécuter des commandes SSH
    private void executeCommand(Session session, String command) throws JSchException, IOException {
        Channel channel = session.openChannel("exec");
        ((ChannelExec)channel).setCommand(command);

        InputStream in = channel.getInputStream();
        InputStream err = ((ChannelExec)channel).getErrStream();

        channel.connect();

        byte[] tmp = new byte[1024];
        while (true) {
            while (in.available() > 0) {
                int i = in.read(tmp, 0, 1024);
                if (i < 0) break;
                System.out.print(new String(tmp, 0, i));
            }
            while (err.available() > 0) {
                int i = err.read(tmp, 0, 1024);
                if (i < 0) break;
                System.err.print(new String(tmp, 0, i));
            }
            if (channel.isClosed()) {
                if (in.available() > 0 || err.available() > 0) continue;
                break;
            }
            try {Thread.sleep(100);} catch (Exception e) {}
        }

        channel.disconnect();
    }

    private String executeCommandWithOutput(Session session, String command) throws JSchException, IOException {
        Channel channel = session.openChannel("exec");
        ((ChannelExec)channel).setCommand(command);

        StringBuilder output = new StringBuilder();
        InputStream in = channel.getInputStream();
        InputStream err = ((ChannelExec)channel).getErrStream();

        channel.connect();

        byte[] tmp = new byte[1024];
        while (true) {
            while (in.available() > 0) {
                int i = in.read(tmp, 0, 1024);
                if (i < 0) break;
                String out = new String(tmp, 0, i);
                output.append(out);
                System.out.print(out);
            }
            while (err.available() > 0) {
                int i = err.read(tmp, 0, 1024);
                if (i < 0) break;
                String error = new String(tmp, 0, i);
                output.append(error);
                System.err.print(error);
            }
            if (channel.isClosed()) {
                if (in.available() > 0 || err.available() > 0) continue;
                break;
            }
            try {Thread.sleep(100);} catch (Exception e) {}
        }

        channel.disconnect();
        return output.toString();
    }

    private void updateBackupInfo(Backup backup, String backupDir) {
        try {
            // Calculer la taille du répertoire de sauvegarde
            long size = calculateDirectorySize(Paths.get(backupDir));
            backup.setFileSize(size);

            // Définir le chemin du fichier
            backup.setFilePath(backupDir);
        } catch (Exception e) {
            // Log l'erreur mais ne pas échouer la sauvegarde pour cette raison
            System.err.println("Erreur lors de la mise à jour des infos de sauvegarde: " + e.getMessage());
        }
    }

    private long calculateDirectorySize(Path directory) {
        try {
            return Files.walk(directory)
                    .filter(p -> p.toFile().isFile())
                    .mapToLong(p -> p.toFile().length())
                    .sum();
        } catch (Exception e) {
            return 0L;
        }
    }
}