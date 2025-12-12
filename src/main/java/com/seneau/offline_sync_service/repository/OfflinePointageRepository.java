package com.seneau.offline_sync_service.repository;

import com.seneau.offline_sync_service.entity.EStatut;
import com.seneau.offline_sync_service.entity.OfflinePointage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OfflinePointageRepository extends JpaRepository<OfflinePointage, Long> {
    List<OfflinePointage> findByStatutAndPublishedAtBefore(EStatut statut, LocalDateTime limite);
}
