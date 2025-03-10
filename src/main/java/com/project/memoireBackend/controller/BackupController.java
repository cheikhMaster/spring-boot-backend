package com.project.memoireBackend.controller;


import com.project.memoireBackend.dto.BackupDTO;
import com.project.memoireBackend.dto.BackupInitiateDTO;
import com.project.memoireBackend.dto.DateRangeDTO;
import com.project.memoireBackend.model.BackupStatus;
import com.project.memoireBackend.model.BackupType;
import com.project.memoireBackend.service.BackupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/backups")
public class BackupController {

    @Autowired
    private BackupService backupService;

    @GetMapping
    public ResponseEntity<List<BackupDTO>> getAllBackups() {
        return ResponseEntity.ok(backupService.getAllBackups());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BackupDTO> getBackupById(@PathVariable Long id) {
        return ResponseEntity.ok(backupService.getBackupById(id));
    }

    @GetMapping("/database/{databaseId}")
    public ResponseEntity<List<BackupDTO>> getBackupsByDatabaseId(@PathVariable Long databaseId) {
        return ResponseEntity.ok(backupService.getBackupsByDatabaseId(databaseId));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<BackupDTO>> getBackupsByStatus(@PathVariable BackupStatus status) {
        return ResponseEntity.ok(backupService.getBackupsByStatus(status));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<BackupDTO>> getBackupsByType(@PathVariable BackupType type) {
        return ResponseEntity.ok(backupService.getBackupsByType(type));
    }

    @PostMapping("/date-range")
    public ResponseEntity<List<BackupDTO>> getBackupsByDateRange(@RequestBody DateRangeDTO dateRange) {
        return ResponseEntity.ok(backupService.getBackupsByDateRange(dateRange.getStartDate(), dateRange.getEndDate()));
    }

    @PostMapping("/initiate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BackupDTO> initiateBackup(@Validated @RequestBody BackupInitiateDTO backupInitiateDTO) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        return ResponseEntity.ok(backupService.initiateBackup(backupInitiateDTO, currentUsername));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BackupDTO> cancelBackup(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        return ResponseEntity.ok(backupService.cancelBackup(id, currentUsername));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteBackup(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        backupService.deleteBackup(id, currentUsername);
        return ResponseEntity.ok().build();
    }
}
