package com.project.memoireBackend.repository;



import com.project.memoireBackend.model.DatabaseInstance;
import com.project.memoireBackend.model.NetworkConnection;
import com.project.memoireBackend.model.NetworkStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NetworkConnectionRepository extends JpaRepository<NetworkConnection, Long> {
    List<NetworkConnection> findBySourceDatabase(DatabaseInstance sourceDatabase);
    List<NetworkConnection> findByTargetDatabase(DatabaseInstance targetDatabase);
    List<NetworkConnection> findByStatus(NetworkStatus status);
    List<NetworkConnection> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT n FROM NetworkConnection n WHERE (n.sourceDatabase = :db1 AND n.targetDatabase = :db2) OR " +
            "(n.sourceDatabase = :db2 AND n.targetDatabase = :db1) ORDER BY n.timestamp DESC")
    List<NetworkConnection> findConnectionsBetweenDatabases(DatabaseInstance db1, DatabaseInstance db2);

    @Query("SELECT n FROM NetworkConnection n WHERE (n.sourceDatabase = :db1 AND n.targetDatabase = :db2) OR " +
            "(n.sourceDatabase = :db2 AND n.targetDatabase = :db1) AND n.timestamp = " +
            "(SELECT MAX(n2.timestamp) FROM NetworkConnection n2 WHERE (n2.sourceDatabase = :db1 AND n2.targetDatabase = :db2) OR " +
            "(n2.sourceDatabase = :db2 AND n2.targetDatabase = :db1))")
    NetworkConnection findLatestConnectionBetweenDatabases(DatabaseInstance db1, DatabaseInstance db2);

    @Query("SELECT AVG(n.latency) FROM NetworkConnection n WHERE (n.sourceDatabase = :db1 AND n.targetDatabase = :db2) OR " +
            "(n.sourceDatabase = :db2 AND n.targetDatabase = :db1) AND n.timestamp BETWEEN :start AND :end")
    Double findAverageLatencyBetweenDatabases(DatabaseInstance db1, DatabaseInstance db2, LocalDateTime start, LocalDateTime end);
}