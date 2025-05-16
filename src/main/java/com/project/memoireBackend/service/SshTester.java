package com.project.memoireBackend.service;

import com.jcraft.jsch.*;
import java.io.InputStream;

import com.jcraft.jsch.*;
import java.io.InputStream;

public class SshTester {
    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: java SshTester <host> <username> <password>");
            System.exit(1);
        }

        String host = args[0];
        String username = args[1];
        String password = args[2];

        System.out.println("Test de connexion SSH vers " + host + " avec utilisateur " + username);

        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(username, host, 22);
            session.setPassword(password);

            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);

            System.out.println("Tentative de connexion...");
            session.connect(30000);
            System.out.println("Connexion établie !");

            // Exécuter une commande simple
            Channel channel = session.openChannel("exec");
            ((ChannelExec)channel).setCommand("echo Test SSH réussi; whoami; hostname");

            channel.setInputStream(null);
            InputStream in = channel.getInputStream();

            channel.connect();

            byte[] tmp = new byte[1024];
            while (true) {
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, 1024);
                    if (i < 0) break;
                    System.out.println(new String(tmp, 0, i));
                }
                if (channel.isClosed()) {
                    System.out.println("Exit status: " + channel.getExitStatus());
                    break;
                }
                try {Thread.sleep(1000);} catch (Exception ee){}
            }

            channel.disconnect();
            session.disconnect();

        } catch (Exception e) {
            System.out.println("Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }
}