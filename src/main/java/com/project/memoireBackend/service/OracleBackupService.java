package com.project.memoireBackend.service;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.Session;
import com.project.memoireBackend.model.Backup;
import com.project.memoireBackend.model.DatabaseInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class OracleBackupService {

    @Value("${app.scripts.directory}")
    private String scriptsDirectory;

    @Autowired
    private SshService sshService;

    public boolean performHotBackup(DatabaseInstance database, Backup backup) {
        if (database.isLocal()) {
            return performLocalOracleBackup(database, backup);
        } else {
            return performRemoteOracleBackup(database, backup);
        }
    }

    private boolean performLocalOracleBackup(DatabaseInstance database, Backup backup) {
        try {
            // Chemin du script de sauvegarde
            String scriptPath = scriptsDirectory + "/oracle_hot_backup.sh";

            // Répertoire de sauvegarde
            String backupDir = "./backups/" + database.getName() + "_" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

            // Commande d'exécution du script
            ProcessBuilder pb = new ProcessBuilder(
                    "bash",
                    scriptPath,
                    database.getName(),  // SID
                    backup.getBackupType().toString(),  // Type de sauvegarde
                    backupDir  // Répertoire de sauvegarde
            );

            // Rediriger la sortie d'erreur vers la sortie standard
            pb.redirectErrorStream(true);

            // Exécuter la commande
            Process process = pb.start();

            // Capturer la sortie du processus
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            // Attendre la fin du processus
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                backup.setFilePath(backupDir);
                backup.setFileSize(calculateBackupSize(backupDir));
                return true;
            } else {
                backup.setErrorMessage("Erreur lors de l'exécution du script: " + output.toString());
                return false;
            }
        } catch (Exception e) {
            backup.setErrorMessage("Erreur lors de la sauvegarde locale: " + e.getMessage());
            return false;
        }
    }

    private boolean performRemoteOracleBackup(DatabaseInstance database, Backup backup) {
        Session sshSession = null;
        try {
            // Établir une connexion SSH
            sshSession = sshService.createSshSession(
                    database.getAddress(),
                    database.getUsername(),
                    "/path/to/ssh_key"
            );

            // Copier le script sur le serveur distant
            String remoteScriptPath = "/tmp/oracle_hot_backup.sh";
            ChannelExec scpChannel = (ChannelExec) sshSession.openChannel("exec");
            scpChannel.setCommand("cat > " + remoteScriptPath + " && chmod +x " + remoteScriptPath);

            try (OutputStream out = scpChannel.getOutputStream()) {
                InputStream scriptStream = new FileInputStream(scriptsDirectory + "/oracle_hot_backup.sh");
                byte[] buffer = new byte[1024];
                int i;
                while ((i = scriptStream.read(buffer, 0, buffer.length)) != -1) {
                    out.write(buffer, 0, i);
                }
                scriptStream.close();
            }

            scpChannel.connect();
            scpChannel.disconnect();

            // Répertoire de sauvegarde sur le serveur distant
            String remoteBackupDir = "/home/" + database.getUsername() + "/oracle_backups/" +
                    database.getName() + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

            // Exécuter le script de sauvegarde
            ChannelExec execChannel = sshService.executeCommand(
                    sshSession,
                    "bash " + remoteScriptPath + " " +
                            database.getName() + " " +  // SID
                            backup.getBackupType().toString() + " " +  // Type de sauvegarde
                            remoteBackupDir  // Répertoire de sauvegarde
            );

            // Lire la sortie
            String output = readChannelOutput(execChannel);
            int exitStatus = execChannel.getExitStatus();
            execChannel.disconnect();

            // Supprimer le script temporaire
            ChannelExec cleanupChannel = sshService.executeCommand(
                    sshSession,
                    "rm " + remoteScriptPath
            );
            cleanupChannel.disconnect();

            if (exitStatus == 0) {
                backup.setFilePath(remoteBackupDir);

                // Récupérer la taille de la sauvegarde
                ChannelExec sizeChannel = sshService.executeCommand(
                        sshSession,
                        "du -sb " + remoteBackupDir + " | awk '{print $1}'"
                );
                String sizeOutput = readChannelOutput(sizeChannel).trim();
                sizeChannel.disconnect();

                try {
                    backup.setFileSize(Long.parseLong(sizeOutput));
                } catch (NumberFormatException e) {
                    backup.setFileSize(0L);
                }

                return true;
            } else {
                backup.setErrorMessage("Erreur lors de l'exécution du script distant: " + output);
                return false;
            }
        } catch (Exception e) {
            backup.setErrorMessage("Erreur lors de la sauvegarde distante: " + e.getMessage());
            return false;
        } finally {
            sshService.closeConnection(sshSession);
        }
    }

    private long calculateBackupSize(String directoryPath) {
        try {
            return Files.walk(Paths.get(directoryPath))
                    .filter(Files::isRegularFile)
                    .mapToLong(p -> {
                        try {
                            return Files.size(p);
                        } catch (IOException e) {
                            return 0L;
                        }
                    })
                    .sum();
        } catch (IOException e) {
            return 0L;
        }
    }

    private String readChannelOutput(ChannelExec channel) throws IOException {
        InputStream in = channel.getInputStream();
        StringBuilder output = new StringBuilder();
        byte[] tmp = new byte[1024];
        while (true) {
            while (in.available() > 0) {
                int i = in.read(tmp, 0, 1024);
                if (i < 0) break;
                output.append(new String(tmp, 0, i));
            }
            if (channel.isClosed()) {
                break;
            }
            try {
                Thread.sleep(100);
            } catch (Exception e) {
                // Ignorer
            }
        }
        return output.toString();
    }
}