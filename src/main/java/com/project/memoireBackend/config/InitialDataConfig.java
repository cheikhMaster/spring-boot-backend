package com.project.memoireBackend.config;


import com.project.memoireBackend.model.User;
import com.project.memoireBackend.model.UserRole;
import com.project.memoireBackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class InitialDataConfig {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
}