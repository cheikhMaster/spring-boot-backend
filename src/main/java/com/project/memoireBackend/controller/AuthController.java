package com.project.memoireBackend.controller;

import com.project.memoireBackend.dto.JwtResponseDTO;
import com.project.memoireBackend.dto.LoginRequestDTO;
import com.project.memoireBackend.dto.UserCreateDTO;
import com.project.memoireBackend.security.JwtTokenProvider;
import com.project.memoireBackend.service.ActivityLogService;
import com.project.memoireBackend.service.UserService;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserService userService;

    @Autowired
    private ActivityLogService activityLogService;

    @PostMapping("/login")
    public ResponseEntity<JwtResponseDTO> authenticateUser(@Validated @RequestBody LoginRequestDTO loginRequest) {
        // Authentifier l'utilisateur
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Générer un token JWT
        String jwt = jwtTokenProvider.generateToken(authentication);

        // Obtenir les détails de l'utilisateur
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // Journaliser la connexion
        activityLogService.logUserActivity(
                userDetails.getUsername(),
                "LOGIN",
                "Connexion de l'utilisateur"
        );

        // Renvoyer la réponse
        return ResponseEntity.ok(new JwtResponseDTO(
                jwt,
                "Bearer",
                null, // Normalement, on récupérerait l'ID à partir de l'UserDetails
                userDetails.getUsername(),
                null, // Email
                userDetails.getAuthorities().iterator().next().getAuthority()
        ));
    }

    @PostMapping("/register")
    public ResponseEntity<JwtResponseDTO> registerUser(@Validated @RequestBody UserCreateDTO userCreateDTO) throws BadRequestException {
        // Cette méthode pourrait être limitée aux administrateurs ou disponible publiquement
        // selon les besoins de l'application

        // Créer un nouvel utilisateur
        userService.createUser(userCreateDTO, "system");

        // Authentifier le nouvel utilisateur
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(userCreateDTO.getUsername(), userCreateDTO.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Générer un token JWT
        String jwt = jwtTokenProvider.generateToken(authentication);

        // Obtenir les détails de l'utilisateur
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // Journaliser l'inscription
        activityLogService.logUserActivity(
                userDetails.getUsername(),
                "REGISTER",
                "Inscription d'un nouvel utilisateur"
        );

        // Renvoyer la réponse
        return ResponseEntity.ok(new JwtResponseDTO(
                jwt,
                "Bearer",
                null,
                userDetails.getUsername(),
                userCreateDTO.getEmail(),
                userDetails.getAuthorities().iterator().next().getAuthority()
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logoutUser() {
        // Récupérer l'utilisateur actuel
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        // Journaliser la déconnexion
        activityLogService.logUserActivity(
                username,
                "LOGOUT",
                "Déconnexion de l'utilisateur"
        );

        // Dans une implémentation réelle avec des tokens JWT, on pourrait ajouter le token
        // à une liste noire ou utiliser une autre stratégie de révocation

        return ResponseEntity.ok().build();
    }
}