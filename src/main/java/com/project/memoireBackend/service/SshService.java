package com.project.memoireBackend.service;

import com.jcraft.jsch.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.function.BiConsumer;

@Service
public class SshService {

    private static final Logger logger = LoggerFactory.getLogger(SshService.class);

    /**
     * Teste la connexion SSH au serveur distant.
     */
    public boolean testConnection(String host, int port, String username, String password) {
        Session session = null;
        Channel channel = null;

        try {
            System.out.println("Test de connexion SSH vers " + host + ":" + port + " avec utilisateur " + username);

            JSch jsch = new JSch();
            session = jsch.getSession(username, host, port);
            session.setPassword(password);

            // Configurer la session
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);

            // Connecter avec timeout
            session.connect(30000);

            // Tester avec une commande simple
            channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand("echo Test_de_connexion_SSH_reussie");

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
                    if (in.available() > 0) continue;
                    int exitStatus = channel.getExitStatus();
                    System.out.println("Test SSH exit status: " + exitStatus);
                    System.out.println("Output: " + output.toString());
                    return exitStatus == 0 && output.toString().contains("Test_de_connexion_SSH_reussie");
                }

                try {Thread.sleep(100);} catch (Exception e) {}
            }
        } catch (Exception e) {
            System.out.println("Erreur lors du test de connexion SSH: " + e.getMessage());
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

    /**
     * Exécute une commande SSH et permet de capturer la sortie standard et d'erreur
     * via un callback
     */
    public boolean executeCommandWithOutput(String host, int port, String username, String password,
                                            String command, BiConsumer<String, String> outputConsumer) {
        Session session = null;
        Channel channel = null;

        try {
            JSch jsch = new JSch();
            session = jsch.getSession(username, host, port);
            session.setPassword(password);

            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            config.put("PreferredAuthentications", "password,keyboard-interactive");
            session.setConfig(config);

            session.connect(30000);

            channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand(command);

            InputStream in = channel.getInputStream();
            InputStream err = ((ChannelExec) channel).getErrStream();
            channel.connect();

            StringBuilder stdOutput = new StringBuilder();
            StringBuilder errOutput = new StringBuilder();
            byte[] tmp = new byte[1024];

            while (true) {
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, 1024);
                    if (i < 0) break;
                    stdOutput.append(new String(tmp, 0, i));
                }

                while (err.available() > 0) {
                    int i = err.read(tmp, 0, 1024);
                    if (i < 0) break;
                    errOutput.append(new String(tmp, 0, i));
                }

                if (channel.isClosed()) {
                    if (in.available() > 0 || err.available() > 0) continue;
                    break;
                }

                try {
                    Thread.sleep(100);
                } catch (Exception ee) {
                    // Ignorer
                }
            }

            // Appeler le callback avec les sorties
            outputConsumer.accept(stdOutput.toString(), errOutput.toString());

            return channel.getExitStatus() == 0;
        } catch (Exception e) {
            logger.error("Erreur lors de l'exécution de la commande: " + e.getMessage(), e);
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

    /**
     * Copie un répertoire distant vers un répertoire local
     */
    public boolean copyRemoteDirectory(String host, int port, String username, String password,
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

            // Obtenir la liste des fichiers dans le répertoire distant
            Channel listChannel = session.openChannel("exec");
            ((ChannelExec)listChannel).setCommand("cmd.exe /c dir /b \"" + remoteDir + "\"");

            InputStream listIn = listChannel.getInputStream();
            listChannel.connect();

            BufferedReader reader = new BufferedReader(new InputStreamReader(listIn));
            String fileName;
            boolean allFilesSucceeded = true;

            while ((fileName = reader.readLine()) != null) {
                fileName = fileName.trim();
                if (!fileName.isEmpty()) {
                    // Copier chaque fichier individuellement
                    boolean success = copyRemoteFile(
                            session,
                            remoteDir + "\\" + fileName,
                            localDir + File.separator + fileName
                    );

                    if (!success) {
                        logger.error("Échec de la copie du fichier: " + fileName);
                        allFilesSucceeded = false;
                    }
                }
            }

            listChannel.disconnect();
            return allFilesSucceeded;
        } catch (Exception e) {
            logger.error("Erreur lors de la copie du répertoire: " + e.getMessage(), e);
            return false;
        } finally {
            if (session != null) {
                session.disconnect();
            }
        }
    }

    /**
     * Copie un fichier distant vers un fichier local via une session SSH existante
     */
    private boolean copyRemoteFile(Session session, String remoteFile, String localFile) {
        Channel channel = null;
        try {
            // Lecture du fichier distant via 'type' sous Windows
            channel = session.openChannel("exec");
            ((ChannelExec)channel).setCommand("cmd.exe /c type \"" + remoteFile + "\"");

            InputStream in = channel.getInputStream();
            channel.connect();

            try (FileOutputStream fos = new FileOutputStream(localFile)) {
                byte[] buffer = new byte[8192];
                int len;

                while (true) {
                    while (in.available() > 0) {
                        len = in.read(buffer);
                        if (len <= 0) break;
                        fos.write(buffer, 0, len);
                    }

                    if (channel.isClosed()) {
                        if (in.available() > 0) continue;
                        break;
                    }

                    try {Thread.sleep(100);} catch (Exception e) {}
                }
            }

            return channel.getExitStatus() == 0;
        } catch (Exception e) {
            logger.error("Erreur lors de la copie du fichier: " + e.getMessage(), e);
            return false;
        } finally {
            if (channel != null) {
                channel.disconnect();
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