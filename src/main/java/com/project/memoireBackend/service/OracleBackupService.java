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
            int remotePort = instance.getPort();
            String remoteUser = instance.getUsername();
            String remotePassword = instance.getPassword();
            String remoteSid = instance.getSid();

            System.out.println("Tentative de sauvegarde distante pour " + instance.getName() +
                    " (" + remoteSid + ") sur " + remoteHost);

            // 3. Créer un nom de répertoire temporaire sur le serveur distant
            String tempDirName = "oracle_backup_" + remoteSid + "_" + System.currentTimeMillis();
            String remoteTempDir = "/tmp/" + tempDirName;

            // 4. Établir une connexion SSH avec le serveur distant
            JSch jsch = new JSch();
            Session session = null;
            try {
                // Créer la session
                session = jsch.getSession(remoteUser, remoteHost, remotePort);
                session.setPassword(remotePassword);

                // Désactiver la vérification des clés connues pour les tests
                java.util.Properties config = new java.util.Properties();
                config.put("StrictHostKeyChecking", "no");
                session.setConfig(config);

                System.out.println("Connexion SSH au serveur distant: " + remoteHost);
                session.connect(30000);

                // 5. Créer le répertoire temporaire sur le serveur distant
                System.out.println("Création du répertoire temporaire sur le serveur distant: " + remoteTempDir);
                Channel execChannel = session.openChannel("exec");
                ((ChannelExec)execChannel).setCommand("mkdir -p " + remoteTempDir);
                execChannel.connect();
                execChannel.disconnect();

                // 6. Copier le script de sauvegarde sur le serveur distant
                String localScriptPath = scriptsDirectory + "/oracle_hot_backup.sh";
                String remoteScriptPath = remoteTempDir + "/oracle_hot_backup.sh";

                System.out.println("Copie du script de sauvegarde vers le serveur distant");
                Channel scpChannel = session.openChannel("exec");
                ((ChannelExec)scpChannel).setCommand("cat > " + remoteScriptPath + " && chmod +x " + remoteScriptPath);
                OutputStream out = scpChannel.getOutputStream();
                FileInputStream fis = new FileInputStream(localScriptPath);
                byte[] buf = new byte[1024];
                int len;

                scpChannel.connect();
                while ((len = fis.read(buf, 0, buf.length)) > 0) {
                    out.write(buf, 0, len);
                }
                fis.close();
                out.close();
                scpChannel.disconnect();

                // 7. Exécuter le script de sauvegarde sur le serveur distant
                String backupType = normalizeBackupType(backup.getBackupType().toString());
                String remoteCommand = "cd " + remoteTempDir + " && " +
                        "export ORACLE_HOME=/u01/app/oracle/product/10.2.0/db_1 && " +
                        "export ORACLE_SID=" + remoteSid + " && " +
                        "./oracle_hot_backup.sh " + remoteSid + " " + backupType + " " + remoteTempDir;

                System.out.println("Exécution du script de sauvegarde sur le serveur distant");
                Channel execChannel2 = session.openChannel("exec");
                ((ChannelExec)execChannel2).setCommand(remoteCommand);

                // Lire la sortie du script
                InputStream in = execChannel2.getInputStream();
                execChannel2.connect();

                byte[] tmp = new byte[1024];
                StringBuilder scriptOutput = new StringBuilder();
                while (true) {
                    while (in.available() > 0) {
                        int i = in.read(tmp, 0, 1024);
                        if (i < 0) break;
                        String output = new String(tmp, 0, i);
                        scriptOutput.append(output);
                        System.out.println("Script distant > " + output);
                    }
                    if (execChannel2.isClosed()) {
                        if (in.available() > 0) continue;
                        System.out.println("Exit status: " + execChannel2.getExitStatus());
                        break;
                    }
                    try {Thread.sleep(1000);} catch (Exception ee) {}
                }

                int exitStatus = execChannel2.getExitStatus();
                execChannel2.disconnect();

                if (exitStatus != 0) {
                    String error = "Échec de la sauvegarde Oracle distante avec code: " + exitStatus;
                    System.out.println(error);
                    backup.setErrorMessage(error);
                    return false;
                }

                // 8. Transférer les fichiers de sauvegarde vers le serveur local
                System.out.println("Compression des fichiers de sauvegarde sur le serveur distant");
                Channel tarChannel = session.openChannel("exec");
                ((ChannelExec)tarChannel).setCommand("cd " + remoteTempDir + " && tar -czf backup.tar.gz *");
                tarChannel.connect();
                tarChannel.disconnect();

                // 9. Télécharger l'archive tar.gz
                System.out.println("Téléchargement des fichiers de sauvegarde vers le serveur local");
                String remoteArchive = remoteTempDir + "/backup.tar.gz";
                String localArchive = backupDir + "/backup.tar.gz";

                Channel downloadChannel = session.openChannel("sftp");
                downloadChannel.connect();
                ChannelSftp channelSftp = (ChannelSftp) downloadChannel;

                channelSftp.get(remoteArchive, localArchive);
                downloadChannel.disconnect();

                // 10. Extraire l'archive localement
                System.out.println("Extraction des fichiers de sauvegarde sur le serveur local");
                ProcessBuilder pbExtract = new ProcessBuilder(
                        "tar", "xzf", localArchive, "-C", backupDir
                );
                Process extractProcess = pbExtract.start();
                int extractExitCode = extractProcess.waitFor();

                if (extractExitCode != 0) {
                    String error = "Échec de l'extraction des fichiers de sauvegarde";
                    System.out.println(error);
                    backup.setErrorMessage(error);
                    return false;
                }

                // 11. Nettoyer les fichiers temporaires sur le serveur distant
                System.out.println("Nettoyage des fichiers temporaires sur le serveur distant");
                Channel cleanupChannel = session.openChannel("exec");
                ((ChannelExec)cleanupChannel).setCommand("rm -rf " + remoteTempDir);
                cleanupChannel.connect();
                cleanupChannel.disconnect();

                // 12. Supprimer l'archive locale
                new File(localArchive).delete();

                System.out.println("Sauvegarde distante terminée avec succès");
                return true;

            } finally {
                if (session != null && session.isConnected()) {
                    session.disconnect();
                }
            }
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