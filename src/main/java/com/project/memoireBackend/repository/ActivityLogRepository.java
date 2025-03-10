package com.project.memoireBackend.repository;


import com.project.memoireBackend.model.ActionType;
import com.project.memoireBackend.model.ActivityLog;
import com.project.memoireBackend.model.DatabaseInstance;
import com.project.memoireBackend.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    List<ActivityLog> findByUser(User user);
    List<ActivityLog> findByDatabaseInstance(DatabaseInstance databaseInstance);
    List<ActivityLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
    List<ActivityLog> findByAction(ActionType action);

    Page<ActivityLog> findByOrderByTimestampDesc(Pageable pageable);
    Page<ActivityLog> findByUserOrderByTimestampDesc(User user, Pageable pageable);
    Page<ActivityLog> findByDatabaseInstanceOrderByTimestampDesc(DatabaseInstance databaseInstance, Pageable pageable);
    Page<ActivityLog> findByActionOrderByTimestampDesc(ActionType action, Pageable pageable);

    long countByUserAndTimestampBetween(User user, LocalDateTime start, LocalDateTime end);
    long countByActionAndTimestampBetween(ActionType action, LocalDateTime start, LocalDateTime end);
}