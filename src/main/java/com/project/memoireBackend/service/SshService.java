package com.project.memoireBackend.service;


import com.jcraft.jsch.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;

@Service
public class SshService {

    private static final Logger logger = LoggerFactory.getLogger(SshService.class);

    @Value("${app.ssh.known-hosts-file:#{null}}")
    private String knownHostsFile;

    @Value("${app.ssh.connection-timeout:30000}")
    private int connectionTimeout;

    /**
     * Crée une session SSH vers un serveur distant
     *
     * @param host Adresse du serveur
     * @param port Port SSH (généralement 22)
     * @param username Nom d'utilisateur
     * @param privateKeyPath Chemin vers la clé privée SSH
     * @return Session SSH établie
     * @throws JSchException En cas d'erreur de connexion SSH
     */
    public Session createSshSession(String host, int port, String username, String privateKeyPath) throws JSchException {
        logger.info("Création d'une session SSH vers {}:{} avec l'utilisateur {}", host, port, username);

        JSch jsch = new JSch();

        // Configurer le fichier known_hosts si spécifié
        if (knownHostsFile != null && !knownHostsFile.isEmpty()) {
            jsch.setKnownHosts(knownHostsFile);
        }

        // Ajouter la clé privée
        jsch.addIdentity(privateKeyPath);

        // Créer la session
        Session session = jsch.getSession(username, host, port);

        // Configurer la session
        java.util.Properties config = new java.util.Properties();
        if (knownHostsFile == null || knownHostsFile.isEmpty()) {
            // Si pas de fichier known_hosts, désactiver la vérification stricte (UNIQUEMENT pour dev/test)
            config.put("StrictHostKeyChecking", "no");
        }
        session.setConfig(config);
        session.setDaemonThread(true);
        session.connect(connectionTimeout);

        logger.info("Session SSH établie avec succès vers {}:{}", host, port);
        return session;
    }

    /**
     * Version simplifiée qui utilise le port SSH par défaut (22)
     */
    public Session createSshSession(String host, String username, String privateKeyPath) throws JSchException {
        return createSshSession(host, 22, username, privateKeyPath);
    }

    /**
     * Exécute une commande sur un serveur distant via SSH
     *
     * @param session Session SSH active
     * @param command Commande à exécuter
     * @return Canal d'exécution pour récupérer la sortie
     * @throws JSchException En cas d'erreur d'exécution
     */
    public ChannelExec executeCommand(Session session, String command) throws JSchException {
        logger.debug("Exécution de la commande via SSH: {}", command);

        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand(command);
        channel.connect();

        return channel;
    }

    /**
     * Lit la sortie standard d'un canal d'exécution SSH
     *
     * @param channel Canal d'exécution
     * @return Contenu de la sortie standard
     * @throws IOException En cas d'erreur de lecture
     */
    public String readChannelOutput(ChannelExec channel) throws IOException {
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
                if (in.available() > 0) continue;
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

    /**
     * Lit la sortie d'erreur d'un canal d'exécution SSH
     *
     * @param channel Canal d'exécution
     * @return Contenu de la sortie d'erreur
     * @throws IOException En cas d'erreur de lecture
     */
    public String readChannelError(ChannelExec channel) throws IOException {
        InputStream err = channel.getErrStream();
        StringBuilder error = new StringBuilder();
        byte[] tmp = new byte[1024];

        while (true) {
            while (err.available() > 0) {
                int i = err.read(tmp, 0, 1024);
                if (i < 0) break;
                error.append(new String(tmp, 0, i));
            }

            if (channel.isClosed()) {
                if (err.available() > 0) continue;
                break;
            }

            try {
                Thread.sleep(100);
            } catch (Exception e) {
                // Ignorer
            }
        }

        return error.toString();
    }

    /**
     * Transfère un fichier local vers un serveur distant via SCP
     *
     * @param session Session SSH active
     * @param localFilePath Chemin du fichier local à transférer
     * @param remoteFilePath Chemin de destination sur le serveur distant
     * @throws Exception En cas d'erreur de transfert
     */
    public void uploadFile(Session session, String localFilePath, String remoteFilePath) throws Exception {
        logger.info("Transfert du fichier {} vers {}", localFilePath, remoteFilePath);

        boolean ptimestamp = true;


        // exec 'scp -t rfile' remotely
        String command = "scp " + (ptimestamp ? "-p" : "") + " -t " + remoteFilePath;
        Channel channel = session.openChannel("exec");
        ((ChannelExec)channel).setCommand(command);

        // get I/O streams for remote scp
        OutputStream out = channel.getOutputStream();
        InputStream in = channel.getInputStream();

        channel.connect();

        if(checkAck(in) != 0) {
            throw new Exception("Erreur lors de l'initialisation du transfert SCP");
        }

        File _lfile = new File(localFilePath);

        if(ptimestamp) {
            command = "T" + (_lfile.lastModified()/1000) + " 0";
            // The access time should be sent here,
            // but it is not accessible with JavaAPI ;-
            command += (" " + (_lfile.lastModified()/1000) + " 0\n");
            out.write(command.getBytes());
            out.flush();

            if(checkAck(in) != 0) {
                throw new Exception("Erreur lors du transfert de l'horodatage");
            }
        }

        // send "C0644 filesize filename", where filename should not include '/'
        long filesize = _lfile.length();
        command = "C0644 " + filesize + " ";
        if(localFilePath.lastIndexOf('/') > 0) {
            command += localFilePath.substring(localFilePath.lastIndexOf('/')+1);
        } else {
            command += localFilePath;
        }
        command += "\n";
        out.write(command.getBytes());
        out.flush();

        if(checkAck(in) != 0) {
            throw new Exception("Erreur lors de l'envoi des informations sur le fichier");
        }
        // Déclaration de buf AVANT le try
        byte[] buf = new byte[1024];

// send file content
        try (FileInputStream fis = new FileInputStream(localFilePath)) {
            int len;
            while ((len = fis.read(buf, 0, buf.length)) > 0) {
                out.write(buf, 0, len);
            }
            out.flush();
        }

// send '\0' (fin du fichier)
        buf[0] = 0;
        out.write(buf, 0, 1);
        out.flush();


        // send a content of lfile
        try (FileInputStream fis = new FileInputStream(localFilePath)) {
            while(true) {
                int len = fis.read(buf, 0, buf.length);
                if(len <= 0) break;
                out.write(buf, 0, len);
            }
            out.flush();
        }

        // send '\0'
        buf[0] = 0;
        out.write(buf, 0, 1);
        out.flush();

        if(checkAck(in) != 0) {
            throw new Exception("Erreur lors du transfert du contenu du fichier");
        }

        channel.disconnect();
        logger.info("Transfert du fichier terminé avec succès");
    }

    /**
     * Vérifie l'accusé de réception SCP
     */
    private int checkAck(InputStream in) throws IOException {
        int b = in.read();
        // b may be 0 for success,
        // 1 for error,
        // 2 for fatal error,
        // -1 for EOF
        if(b == 0) return 0;
        if(b == -1) return -1;

        if(b == 1 || b == 2) {
            StringBuilder sb = new StringBuilder();
            int c;
            do {
                c = in.read();
                sb.append((char)c);
            } while(c != '\n');

            if(b == 1) { // error
                logger.error("SCP error: {}", sb.toString());
            }
            if(b == 2) { // fatal error
                logger.error("SCP fatal error: {}", sb.toString());
            }
        }
        return b;
    }

    /**
     * Télécharge un fichier depuis un serveur distant via SCP
     *
     * @param session Session SSH active
     * @param remoteFilePath Chemin du fichier sur le serveur distant
     * @param localFilePath Chemin de destination local
     * @throws Exception En cas d'erreur de téléchargement
     */
    public void downloadFile(Session session, String remoteFilePath, String localFilePath) throws Exception {
        logger.info("Téléchargement du fichier {} vers {}", remoteFilePath, localFilePath);

        String prefix = null;
        if(new File(localFilePath).isDirectory()) {
            prefix = localFilePath + File.separator;
        }

        // exec 'scp -f rfile' remotely
        String command = "scp -f " + remoteFilePath;
        Channel channel = session.openChannel("exec");
        ((ChannelExec)channel).setCommand(command);

        // get I/O streams for remote scp
        OutputStream out = channel.getOutputStream();
        InputStream in = channel.getInputStream();

        channel.connect();

        byte[] buf = new byte[1024];

        // send '\0'
        buf[0] = 0;
        out.write(buf, 0, 1);
        out.flush();

        while(true) {
            int c = checkAck(in);
            if(c != 'C') {
                break;
            }

            // read '0644 '
            in.read(buf, 0, 5);

            long filesize = 0L;
            while(true) {
                if(in.read(buf, 0, 1) < 0) {
                    // error
                    break;
                }
                if(buf[0] == ' ') break;
                filesize = filesize * 10L + (long)(buf[0] - '0');
            }

            String file = null;
            for(int i = 0;; i++) {
                in.read(buf, i, 1);
                if(buf[i] == (byte)0x0a) {
                    file = new String(buf, 0, i);
                    break;
                }
            }

            // send '\0'
            buf[0] = 0;
            out.write(buf, 0, 1);
            out.flush();

            // read a content of lfile
            try (FileOutputStream fos = new FileOutputStream(prefix == null ? localFilePath : prefix + file)) {
                int foo;
                while(true) {
                    if(buf.length < filesize) foo = buf.length;
                    else foo = (int)filesize;
                    foo = in.read(buf, 0, foo);
                    if(foo < 0) {
                        // error
                        break;
                    }
                    fos.write(buf, 0, foo);
                    filesize -= foo;
                    if(filesize == 0L) break;
                }
            }

            if(checkAck(in) != 0) {
                throw new Exception("Erreur lors du téléchargement du fichier");
            }

            // send '\0'
            buf[0] = 0;
            out.write(buf, 0, 1);
            out.flush();
        }

        channel.disconnect();
        logger.info("Téléchargement du fichier terminé avec succès");
    }

    /**
     * Ferme une session SSH
     *
     * @param session Session à fermer
     */
    public void closeConnection(Session session) {
        if (session != null && session.isConnected()) {
            logger.info("Fermeture de la session SSH");
            session.disconnect();
        }
    }
}