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
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(username, host, port);
            session.setPassword(password);

            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);

            session.connect(30000);
            return true;
        } catch (Exception e) {
            logger.error("Erreur de connexion SSH: " + e.getMessage());
            return false;
        } finally {
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
                    logger.debug("Command output: " + output.toString());
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

    public boolean uploadFile(String host, int port, String username, String password,
                              String localFilePath, String remoteFilePath) {
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

            channel = session.openChannel("sftp");
            channel.connect();
            ChannelSftp sftpChannel = (ChannelSftp) channel;

            // Créer le répertoire parent si nécessaire
            String remoteDir = remoteFilePath.substring(0, remoteFilePath.lastIndexOf('/'));
            try {
                createRemoteDirectories(sftpChannel, remoteDir);
            } catch (SftpException e) {
                logger.warn("Erreur lors de la création des répertoires distants: " + e.getMessage());
            }

            // Téléverser le fichier
            sftpChannel.put(new FileInputStream(localFilePath), remoteFilePath);

            return true;
        } catch (Exception e) {
            logger.error("Erreur lors du téléversement du fichier: " + e.getMessage());
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

    public boolean downloadDirectory(String host, int port, String username, String password,
                                     String remoteDir, String localDir) {
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

            channel = session.openChannel("sftp");
            channel.connect();
            ChannelSftp sftpChannel = (ChannelSftp) channel;

            // S'assurer que le répertoire local existe
            File localDirFile = new File(localDir);
            if (!localDirFile.exists()) {
                localDirFile.mkdirs();
            }

            // Télécharger récursivement le répertoire
            downloadDirectoryContent(sftpChannel, remoteDir, localDir);

            return true;
        } catch (Exception e) {
            logger.error("Erreur lors du téléchargement du répertoire: " + e.getMessage());
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

    private void createRemoteDirectories(ChannelSftp sftpChannel, String remoteDir) throws SftpException {
        if (remoteDir.equals(""))
            return;

        try {
            sftpChannel.cd(remoteDir);
            return;
        } catch (SftpException e) {
            // Le répertoire n'existe pas, nous devons le créer
        }

        int lastSeparator = remoteDir.lastIndexOf('/');
        if (lastSeparator != -1) {
            // Créer le parent d'abord
            String parentDir = remoteDir.substring(0, lastSeparator);
            createRemoteDirectories(sftpChannel, parentDir);
        }

        if (remoteDir.length() > 0) {
            try {
                sftpChannel.mkdir(remoteDir);
            } catch (SftpException e) {
                // Peut-être que quelqu'un d'autre a créé le répertoire entre-temps
                if (e.id != ChannelSftp.SSH_FX_FAILURE)
                    throw e;
            }
        }
    }

    private void downloadDirectoryContent(ChannelSftp sftpChannel, String remoteDir, String localDir)
            throws SftpException, IOException {
        File localDirFile = new File(localDir);
        if (!localDirFile.exists()) {
            localDirFile.mkdirs();
        }

        sftpChannel.cd(remoteDir);

        java.util.Vector<ChannelSftp.LsEntry> list = sftpChannel.ls(".");
        for (ChannelSftp.LsEntry entry : list) {
            String filename = entry.getFilename();

            if (entry.getAttrs().isDir()) {
                if (!".".equals(filename) && !"..".equals(filename)) {
                    String newRemoteDir = remoteDir + "/" + filename;
                    String newLocalDir = localDir + "/" + filename;
                    downloadDirectoryContent(sftpChannel, newRemoteDir, newLocalDir);
                }
            } else {
                OutputStream outputStream = new FileOutputStream(localDir + "/" + filename);
                sftpChannel.get(filename, outputStream);
                outputStream.close();
            }
        }

        // Revenir au répertoire parent
        sftpChannel.cd("..");
    }
}