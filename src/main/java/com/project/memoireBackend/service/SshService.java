package com.project.memoireBackend.service;

import com.jcraft.jsch.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;


@Service
public class SshService {

    private static final Logger logger = LoggerFactory.getLogger(SshService.class);

    public boolean testConnection(String host, int port, String username, String password) {
        Session session = null;
        Channel channel = null;

        try {
            System.out.println("Test de connexion SSH détaillé:");
            System.out.println("- Hôte: " + host);
            System.out.println("- Port: " + port);
            System.out.println("- Utilisateur: " + username);

            JSch jsch = new JSch();
            session = jsch.getSession(username, host, port);
            session.setPassword(password);

            // Configuration améliorée
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            config.put("PreferredAuthentications", "password,keyboard-interactive");
            config.put("MaxAuthTries", "5");
            config.put("ConnectionAttempts", "3");
            session.setConfig(config);

            System.out.println("Tentative de connexion SSH avec timeout de 30 secondes...");
            session.connect(30000);
            System.out.println("Connexion SSH établie !");

            // Tester l'exécution d'une commande simple
            channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand("echo 'Test de connexion SSH réussi'");

            InputStream in = channel.getInputStream();
            channel.connect();

            byte[] tmp = new byte[1024];
            StringBuilder output = new StringBuilder();

            while (true) {
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, 1024);
                    if (i < 0) break;
                    output.append(new String(tmp, 0, i));
                }

                if (channel.isClosed()) {
                    int exitStatus = channel.getExitStatus();
                    System.out.println("Commande terminée avec code: " + exitStatus);
                    System.out.println("Sortie: '" + output.toString() + "'");

                    return exitStatus == 0;
                }

                try {Thread.sleep(100);} catch (Exception e) {}
            }
        } catch (Exception e) {
            logger.error("Erreur lors du test de connexion SSH: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    public boolean executeCommand(String host, int port, String username, String password, String command) {
        Session session = null;
        Channel channel = null;

        try {
            JSch jsch = new JSch();
            session = jsch.getSession(username, host, port);
            session.setPassword(password);

            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);

            session.connect(30000);

            channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand(command);

            InputStream in = channel.getInputStream();
            InputStream err = ((ChannelExec) channel).getErrStream();
            channel.connect();

            StringBuilder output = new StringBuilder();
            byte[] tmp = new byte[1024];

            while (true) {
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, 1024);
                    if (i < 0) break;
                    output.append(new String(tmp, 0, i));
                }

                while (err.available() > 0) {
                    int i = err.read(tmp, 0, 1024);
                    if (i < 0) break;
                    output.append(new String(tmp, 0, i));
                }

                if (channel.isClosed()) {
                    if (in.available() > 0 || err.available() > 0) continue;
                    logger.info("Exit status: " + channel.getExitStatus());
                    logger.debug("Command output: " + output);
                    break;
                }

                try {
                    Thread.sleep(100);
                } catch (Exception ee) {
                    // Ignorer
                }
            }

            return channel.getExitStatus() == 0;
        } catch (Exception e) {
            logger.error("Erreur lors de l'exécution de la commande: " + e.getMessage());
            return false;
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
        }
    }

    // Méthode pour télécharger un fichier distant
    public boolean downloadFile(String host, int port, String username, String password,
                                String remoteFilePath, String localFilePath) {
        Session session = null;
        Channel channel = null;

        try {
            JSch jsch = new JSch();
            session = jsch.getSession(username, host, port);
            session.setPassword(password);

            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);

            session.connect();

            // Créer un canal SFTP
            channel = session.openChannel("sftp");
            channel.connect();
            ChannelSftp sftpChannel = (ChannelSftp) channel;

            // Convertir le chemin pour SFTP (remplacer les \ par des /)
            remoteFilePath = remoteFilePath.replace('\\', '/');

            // Télécharger le fichier
            sftpChannel.get(remoteFilePath, localFilePath);

            return true;
        } catch (Exception e) {
            logger.error("Erreur lors du téléchargement du fichier: " + e.getMessage());
            return false;
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
        }
    }

    // Méthode pour copier un répertoire distant via exécution de commandes
    public boolean copyFilesViaExec(String host, int port, String username, String password,
                                    String remoteDir, String localDir) {
        Session session = null;
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(username, host, port);
            session.setPassword(password);

            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);

            session.connect();

            // S'assurer que le répertoire local existe
            File localDirFile = new File(localDir);
            if (!localDirFile.exists()) {
                localDirFile.mkdirs();
            }

            // Obtenir la liste des fichiers
            Channel listChannel = session.openChannel("exec");
            ((ChannelExec)listChannel).setCommand("cmd.exe /c dir /b \"" + remoteDir + "\"");

            InputStream listIn = listChannel.getInputStream();
            listChannel.connect();

            BufferedReader reader = new BufferedReader(new InputStreamReader(listIn));
            String fileName;
            while ((fileName = reader.readLine()) != null) {
                fileName = fileName.trim();
                if (!fileName.isEmpty()) {
                    // Pour chaque fichier, récupérer son contenu
                    Channel fileChannel = session.openChannel("exec");
                    ((ChannelExec)fileChannel).setCommand("cmd.exe /c type \"" + remoteDir + "\\" + fileName + "\"");

                    InputStream fileIn = fileChannel.getInputStream();
                    fileChannel.connect();

                    // Écrire le contenu dans un fichier local
                    File localFile = new File(localDir, fileName);
                    FileOutputStream fos = new FileOutputStream(localFile);

                    byte[] buffer = new byte[1024];
                    int len;
                    while (true) {
                        while (fileIn.available() > 0) {
                            len = fileIn.read(buffer);
                            if (len <= 0) break;
                            fos.write(buffer, 0, len);
                        }
                        if (fileChannel.isClosed()) {
                            if (fileIn.available() > 0) continue;
                            break;
                        }
                        try {Thread.sleep(100);} catch (Exception e) {}
                    }

                    fos.close();
                    fileChannel.disconnect();
                }
            }

            listChannel.disconnect();
            return true;
        } catch (Exception e) {
            logger.error("Erreur lors de la copie des fichiers: " + e.getMessage());
            return false;
        } finally {
            if (session != null) {
                session.disconnect();
            }
        }
    }
}