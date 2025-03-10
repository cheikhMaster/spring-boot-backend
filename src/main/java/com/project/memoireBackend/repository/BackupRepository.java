package com.project.memoireBackend.repository;


import com.project.memoireBackend.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BackupRepository extends JpaRepository<Backup, Long> {
    List<Backup> findByDatabaseInstance(DatabaseInstance databaseInstance);
    List<Backup> findByStatus(BackupStatus status);
    List<Backup> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);
    List<Backup> findByCreatedBy(User user);
    List<Backup> findByBackupType(BackupType backupType);

    List<Backup> findByDatabaseInstanceAndStatus(DatabaseInstance databaseInstance, BackupStatus status);
    List<Backup> findByStatusAndStartTimeBefore(BackupStatus status, LocalDateTime time);

    long countByDatabaseInstance(DatabaseInstance databaseInstance);
    long countByStatus(BackupStatus status);
}