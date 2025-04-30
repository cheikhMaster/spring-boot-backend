package com.project.memoireBackend.config;

<<<<<<< HEAD


=======
>>>>>>> aa10145 (commit du 30/04 13 50)
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        // Autoriser toutes les origines pour le développement
        // En production, limitez-le à votre domaine frontend
        config.addAllowedOrigin("http://localhost:4200");

        // Autoriser les en-têtes courants
        config.addAllowedHeader("*");

        // Autoriser tous les méthodes HTTP communes
        config.addAllowedMethod("GET");
        config.addAllowedMethod("POST");
        config.addAllowedMethod("PUT");
        config.addAllowedMethod("DELETE");
        config.addAllowedMethod("PATCH");
        config.addAllowedMethod("OPTIONS");

        // Autoriser les cookies
        config.setAllowCredentials(true);

        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}