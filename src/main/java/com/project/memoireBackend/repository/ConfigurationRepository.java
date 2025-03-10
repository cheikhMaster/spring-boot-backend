package com.project.memoireBackend.repository;

import com.project.memoireBackend.model.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConfigurationRepository extends JpaRepository<Configuration, Long> {
    Optional<Configuration> findByConfigKey(String configKey);
    boolean existsByConfigKey(String configKey);
    void deleteByConfigKey(String configKey);
}