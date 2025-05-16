package com.project.memoireBackend.service;

import com.project.memoireBackend.model.DatabaseInstance;
import com.project.memoireBackend.model.DatabaseStatus;
import com.project.memoireBackend.repository.DatabaseInstanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class MetricSchedulerService {

    @Autowired
    private DatabaseInstanceRepository databaseInstanceRepository;

    @Autowired
    private OracleMetricCollector oracleMetricCollector;

    @Autowired
    private MySQLMetricCollector mySQLMetricCollector;

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    // Exécution toutes les 5 minutes
    @Scheduled(cron = "0 */5 * * * *")
    public void collectMetricsForAllDatabases() {
        System.out.println("Début de la collecte des métriques pour toutes les bases de données");

        // Récupérer toutes les bases de données actives
        List<DatabaseInstance> activeDatabases = databaseInstanceRepository.findByStatus(DatabaseStatus.ACTIVE);

        // Collecter les métriques en parallèle
        List<CompletableFuture<Void>> futures = activeDatabases.stream()
                .map(database -> CompletableFuture.runAsync(() -> {
                    try {
                        collectMetricsForDatabase(database);
                    } catch (Exception e) {
                        System.err.println("Erreur lors de la collecte des métriques pour " +
                                database.getName() + ": " + e.getMessage());
                    }
                }, executorService))
                .toList();

        // Attendre que toutes les collectes soient terminées
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        System.out.println("Fin de la collecte des métriques");
    }

    // Exécution toutes les heures pour les métriques moins fréquentes
    @Scheduled(cron = "0 0 * * * *")
    public void collectDetailedMetrics() {
        System.out.println("Début de la collecte des métriques détaillées");

        List<DatabaseInstance> activeDatabases = databaseInstanceRepository.findByStatus(DatabaseStatus.ACTIVE);

        for (DatabaseInstance database : activeDatabases) {
            try {
                // Collecter des métriques plus détaillées ou moins fréquentes
                collectDetailedMetricsForDatabase(database);
            } catch (Exception e) {
                System.err.println("Erreur lors de la collecte des métriques détaillées pour " +
                        database.getName() + ": " + e.getMessage());
            }
        }

        System.out.println("Fin de la collecte des métriques détaillées");
    }

    // Nettoyage des anciennes métriques (exécution quotidienne à 2h du matin)
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupOldMetrics() {
        System.out.println("Début du nettoyage des anciennes métriques");

        try {
            // Supprimer les métriques de plus de 30 jours
            // (À implémenter selon votre politique de rétention)
        } catch (Exception e) {
            System.err.println("Erreur lors du nettoyage des métriques: " + e.getMessage());
        }

        System.out.println("Fin du nettoyage des anciennes métriques");
    }

    private void collectMetricsForDatabase(DatabaseInstance database) {
        System.out.println("Collecte des métriques pour: " + database.getName());

        switch (database.getType()) {
            case ORACLE:
                oracleMetricCollector.collectMetrics(database);
                break;
            case MYSQL:
                mySQLMetricCollector.collectMetrics(database);
                break;
            case POSTGRES:
                // À implémenter
                break;
            case SQLSERVER:
                // À implémenter
                break;
            default:
                System.out.println("Type de base de données non supporté pour le monitoring: " + database.getType());
        }
    }

    private void collectDetailedMetricsForDatabase(DatabaseInstance database) {
        // Implémenter la collecte de métriques détaillées
        // Par exemple, analyse des requêtes lentes, statistiques d'index, etc.
    }
}