package com.seneau.offline_sync_service.data.repository;

import com.seneau.offline_sync_service.data.model.SyncBatch;
import com.seneau.offline_sync_service.data.model.SyncStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SyncBatchRepository extends JpaRepository<SyncBatch, Long> {

    /**
     * Trouver un batch par son ID unique
     */
    Optional<SyncBatch> findByBatchId(String batchId);

    /**
     * Trouver les batchs d'un utilisateur avec certains statuts
     */
    List<SyncBatch> findByEmailAndStatusIn(String email, List<SyncStatus> statuses);

    /**
     * Trouver tous les batchs d'un utilisateur
     */
    List<SyncBatch> findByEmail(String email);

    /**
     * Trouver par statut
     */
    List<SyncBatch> findByStatus(SyncStatus status);

    /**
     * Trouver par type de pointage
     */
    List<SyncBatch> findByTypePointage(String typePointage);

    /**
     * Trouver par email et type
     */
    List<SyncBatch> findByEmailAndTypePointage(String email, String typePointage);

    /**
     * Pagination des batchs par email
     */
    Page<SyncBatch> findByEmailOrderByCreatedAtDesc(String email, Pageable pageable);

    /**
     * Trouver les batchs dans une plage de dates
     */
    @Query("SELECT sb FROM SyncBatch sb WHERE sb.email = :email " +
           "AND sb.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY sb.createdAt DESC")
    List<SyncBatch> findByEmailAndDateRange(
            @Param("email") String email,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Trouver les batchs anciens par statut
     */
    @Query("SELECT sb FROM SyncBatch sb WHERE sb.status = :status " +
           "AND sb.createdAt < :beforeDate")
    List<SyncBatch> findOldBatchesByStatus(
            @Param("status") SyncStatus status,
            @Param("beforeDate") LocalDateTime beforeDate
    );

    /**
     * Compter les batchs en attente
     */
    @Query("SELECT COUNT(sb) FROM SyncBatch sb WHERE sb.email = :email " +
           "AND sb.status IN ('PENDING', 'IN_PROGRESS', 'RETRY')")
    Long countPendingBatchesByEmail(@Param("email") String email);

    // ========== STATISTIQUES ==========

    /**
     * Compter les synchronisations réussies
     */
    @Query("SELECT COUNT(sb) FROM SyncBatch sb WHERE sb.email = :email " +
           "AND sb.status = 'COMPLETED'")
    Long countSuccessfulSyncsByEmail(@Param("email") String email);

    /**
     * Compter le total de pointages synchronisés
     */
    @Query("SELECT COALESCE(SUM(sb.totalPointages), 0) FROM SyncBatch sb " +
           "WHERE sb.email = :email AND sb.status = 'COMPLETED'")
    Long countTotalPointagesSyncedByEmail(@Param("email") String email);

    /**
     * Calculer le taux d'échec moyen
     */
    @Query("SELECT COALESCE(AVG(CAST(sb.failureCount AS double) / sb.totalPointages * 100), 0) " +
           "FROM SyncBatch sb WHERE sb.email = :email AND sb.totalPointages > 0")
    Double averageFailureRateByEmail(@Param("email") String email);

    /**
     * Calculer la durée moyenne de traitement
     */
    @Query("SELECT AVG(EXTRACT(EPOCH FROM (sb.completedAt - sb.createdAt))) " +
           "FROM SyncBatch sb WHERE sb.email = :email " +
           "AND sb.completedAt IS NOT NULL")
    Double averageDurationSecondsByEmail(@Param("email") String email);

    /**
     * Statistiques globales par type
     */
    @Query("SELECT sb.typePointage, COUNT(sb), SUM(sb.successCount), SUM(sb.failureCount) " +
           "FROM SyncBatch sb WHERE sb.email = :email " +
           "GROUP BY sb.typePointage")
    List<Object[]> getStatsByTypeForEmail(@Param("email") String email);

    /**
     * Trouver les batchs avec le plus d'échecs
     */
    @Query("SELECT sb FROM SyncBatch sb WHERE sb.failureCount > 0 " +
           "ORDER BY sb.failureCount DESC")
    List<SyncBatch> findBatchesWithMostFailures(Pageable pageable);

    // ========== NETTOYAGE ==========

    /**
     * Supprimer les batchs avant une date
     */
    void deleteByCreatedAtBefore(LocalDateTime date);

    /**
     * Supprimer les batchs complétés avant une date
     */
    @Query("DELETE FROM SyncBatch sb WHERE sb.status = :status " +
           "AND sb.createdAt < :date")
    void deleteByStatusAndCreatedAtBefore(
            @Param("status") SyncStatus status,
            @Param("date") LocalDateTime date
    );

    /**
     * Vérifier l'existence d'un batch
     */
    boolean existsByBatchId(String batchId);

    /**
     * Compter les batchs par statut
     */
    Long countByStatus(SyncStatus status);

    /**
     * Compter les batchs par type
     */
    Long countByTypePointage(String typePointage);
}