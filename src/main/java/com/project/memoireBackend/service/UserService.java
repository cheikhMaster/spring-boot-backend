package com.project.memoireBackend.service;


import com.project.memoireBackend.dto.PasswordUpdateDTO;
import com.project.memoireBackend.dto.UserCreateDTO;
import com.project.memoireBackend.dto.UserDTO;
import com.project.memoireBackend.excepton.ResourceNotFoundException;
import com.project.memoireBackend.model.User;
import com.project.memoireBackend.model.UserRole;
import com.project.memoireBackend.repository.UserRepository;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ActivityLogService activityLogService;

    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec l'id: " + id));
        return convertToDTO(user);
    }

    public UserDTO getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec le nom: " + username));
        return convertToDTO(user);
    }

    public List<UserDTO> getUsersByRole(UserRole role) {
        return userRepository.findByRole(role).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<UserDTO> getActiveUsers() {
        return userRepository.findByActive(true).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserDTO createUser(UserCreateDTO userCreateDTO, String createdByUsername) throws BadRequestException {
        if (userRepository.existsByUsername(userCreateDTO.getUsername())) {
            throw new BadRequestException("Ce nom d'utilisateur est déjà utilisé");
        }

        User user = new User();
        user.setUsername(userCreateDTO.getUsername());
        user.setPassword(passwordEncoder.encode(userCreateDTO.getPassword()));
        user.setEmail(userCreateDTO.getEmail());
        user.setRole(userCreateDTO.getRole());
        user.setActive(userCreateDTO.isActive());

        User savedUser = userRepository.save(user);

        // Journalisation
        activityLogService.logUserActivity(
                createdByUsername,
                "CREATE_USER",
                "Création de l'utilisateur: " + user.getUsername()
        );

        return convertToDTO(savedUser);
    }

    @Transactional
    public UserDTO updateUser(Long id, UserCreateDTO userDetails, String updatedByUsername) throws BadRequestException {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec l'id: " + id));

        // Vérifiez si le nom d'utilisateur est modifié et s'il est déjà utilisé
        if (!user.getUsername().equals(userDetails.getUsername()) &&
                userRepository.existsByUsername(userDetails.getUsername())) {
            throw new BadRequestException("Ce nom d'utilisateur est déjà utilisé");
        }

        user.setUsername(userDetails.getUsername());
        user.setEmail(userDetails.getEmail());
        user.setRole(userDetails.getRole());
        user.setActive(userDetails.isActive());

        // Mettre à jour le mot de passe seulement s'il est fourni
        if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(userDetails.getPassword()));
        }

        User updatedUser = userRepository.save(user);

        // Journalisation
        activityLogService.logUserActivity(
                updatedByUsername,
                "MODIFY_USER",
                "Mise à jour de l'utilisateur: " + user.getUsername()
        );

        return convertToDTO(updatedUser);
    }

    @Transactional
    public UserDTO updatePassword(Long id, PasswordUpdateDTO passwordUpdate, String currentUsername) throws BadRequestException {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec l'id: " + id));

        // Vérifier si l'ancien mot de passe est correct
        if (!passwordEncoder.matches(passwordUpdate.getOldPassword(), user.getPassword())) {
            throw new BadRequestException("L'ancien mot de passe est incorrect");
        }

        // Vérifier si la confirmation correspond au nouveau mot de passe
        if (!passwordUpdate.getNewPassword().equals(passwordUpdate.getConfirmPassword())) {
            throw new BadRequestException("La confirmation du mot de passe ne correspond pas");
        }

        // Mettre à jour le mot de passe
        user.setPassword(passwordEncoder.encode(passwordUpdate.getNewPassword()));
        User updatedUser = userRepository.save(user);

        // Journalisation
        activityLogService.logUserActivity(
                currentUsername,
                "CHANGE_PASSWORD",
                "Changement de mot de passe pour l'utilisateur: " + user.getUsername()
        );

        return convertToDTO(updatedUser);
    }

    @Transactional
    public void deleteUser(Long id, String deletedByUsername) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec l'id: " + id));

        String username = user.getUsername();
        userRepository.delete(user);

        // Journalisation
        activityLogService.logUserActivity(
                deletedByUsername,
                "DELETE_USER",
                "Suppression de l'utilisateur: " + username
        );
    }

    @Transactional
    public UserDTO toggleUserActive(Long id, String updatedByUsername) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec l'id: " + id));

        user.setActive(!user.isActive());
        User updatedUser = userRepository.save(user);

        // Journalisation
        String action = user.isActive() ? "ACTIVATE_USER" : "DEACTIVATE_USER";
        String details = user.isActive() ? "Activation de l'utilisateur: " : "Désactivation de l'utilisateur: ";

        activityLogService.logUserActivity(
                updatedByUsername,
                action,
                details + user.getUsername()
        );

        return convertToDTO(updatedUser);
    }

    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        dto.setActive(user.isActive());
        return dto;
    }
}