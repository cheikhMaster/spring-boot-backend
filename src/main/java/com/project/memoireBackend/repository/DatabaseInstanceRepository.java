package com.project.memoireBackend.repository;


import com.project.memoireBackend.model.DatabaseInstance;
import com.project.memoireBackend.model.DatabaseStatus;
import com.project.memoireBackend.model.DatabaseType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DatabaseInstanceRepository extends JpaRepository<DatabaseInstance, Long> {
    List<DatabaseInstance> findByIsLocal(boolean isLocal);
    List<DatabaseInstance> findByStatus(DatabaseStatus status);
    List<DatabaseInstance> findByType(DatabaseType type);
    List<DatabaseInstance> findByNameContainingIgnoreCase(String name);
}
