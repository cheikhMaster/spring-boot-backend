package com.project.memoireBackend.service;


import com.project.memoireBackend.dto.ActivityLogDTO;
import com.project.memoireBackend.excepton.ResourceNotFoundException;
import com.project.memoireBackend.model.ActionType;
import com.project.memoireBackend.model.ActivityLog;
import com.project.memoireBackend.model.DatabaseInstance;
import com.project.memoireBackend.model.User;
import com.project.memoireBackend.repository.ActivityLogRepository;
import com.project.memoireBackend.repository.DatabaseInstanceRepository;
import com.project.memoireBackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ActivityLogService {

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DatabaseInstanceRepository databaseInstanceRepository;

    public Page<ActivityLogDTO> getAllActivityLogs(Pageable pageable) {
        return activityLogRepository.findByOrderByTimestampDesc(pageable)
                .map(this::convertToDTO);
    }

    public Page<ActivityLogDTO> getActivityLogsByUser(String username, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec le nom: " + username));

        return activityLogRepository.findByUserOrderByTimestampDesc(user, pageable)
                .map(this::convertToDTO);
    }

    public Page<ActivityLogDTO> getActivityLogsByDatabase(Long databaseId, Pageable pageable) {
        DatabaseInstance instance = databaseInstanceRepository.findById(databaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Base de données non trouvée avec l'id: " + databaseId));

        return activityLogRepository.findByDatabaseInstanceOrderByTimestampDesc(instance, pageable)
                .map(this::convertToDTO);
    }

    public Page<ActivityLogDTO> getActivityLogsByAction(ActionType action, Pageable pageable) {
        return activityLogRepository.findByActionOrderByTimestampDesc(action, pageable)
                .map(this::convertToDTO);
    }

    public List<ActivityLogDTO> getActivityLogsByDateRange(LocalDateTime start, LocalDateTime end) {
        return activityLogRepository.findByTimestampBetween(start, end).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void logUserActivity(String username, String actionType, String details) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec le nom: " + username));

        ActivityLog log = new ActivityLog();
        log.setTimestamp(LocalDateTime.now());
        log.setUser(user);
        log.setAction(ActionType.valueOf(actionType));
        log.setDetails(details);

        activityLogRepository.save(log);
    }

    @Transactional
    public void logDatabaseActivity(String username, String actionType, String details, Long databaseId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec le nom: " + username));

        DatabaseInstance instance = databaseInstanceRepository.findById(databaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Base de données non trouvée avec l'id: " + databaseId));

        ActivityLog log = new ActivityLog();
        log.setTimestamp(LocalDateTime.now());
        log.setUser(user);
        log.setAction(ActionType.valueOf(actionType));
        log.setDetails(details);
        log.setDatabaseInstance(instance);

        activityLogRepository.save(log);
    }

    @Transactional
    public void logBackupActivity(String username, String actionType, String details,
                                  Long databaseId, Long backupId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec le nom: " + username));

        DatabaseInstance instance = databaseInstanceRepository.findById(databaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Base de données non trouvée avec l'id: " + databaseId));

        ActivityLog log = new ActivityLog();
        log.setTimestamp(LocalDateTime.now());
        log.setUser(user);
        log.setAction(ActionType.valueOf(actionType));
        log.setDetails(details + (backupId != null ? " (ID sauvegarde: " + backupId + ")" : ""));
        log.setDatabaseInstance(instance);

        activityLogRepository.save(log);
    }

    @Transactional
    public void logMetricActivity(String username, String actionType, String details,
                                  Long databaseId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec le nom: " + username));

        DatabaseInstance instance = databaseInstanceRepository.findById(databaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Base de données non trouvée avec l'id: " + databaseId));

        ActivityLog log = new ActivityLog();
        log.setTimestamp(LocalDateTime.now());
        log.setUser(user);
        log.setAction(ActionType.valueOf(actionType));
        log.setDetails(details);
        log.setDatabaseInstance(instance);

        activityLogRepository.save(log);
    }

    private ActivityLogDTO convertToDTO(ActivityLog log) {
        ActivityLogDTO dto = new ActivityLogDTO();
        dto.setId(log.getId());
        dto.setTimestamp(log.getTimestamp());

        if (log.getUser() != null) {
            dto.setUserId(log.getUser().getId());
            dto.setUsername(log.getUser().getUsername());
        }

        dto.setAction(log.getAction());
        dto.setDetails(log.getDetails());

        if (log.getDatabaseInstance() != null) {
            dto.setDatabaseId(log.getDatabaseInstance().getId());
            dto.setDatabaseName(log.getDatabaseInstance().getName());
        }

        dto.setIpAddress(log.getIpAddress());

        return dto;
    }
}