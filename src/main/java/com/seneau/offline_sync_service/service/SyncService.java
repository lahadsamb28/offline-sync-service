package com.seneau.offline_sync_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seneau.offline_sync_service.dto.PointageBatchRequestDto;
import com.seneau.offline_sync_service.entity.EStatut;
import com.seneau.offline_sync_service.entity.OfflinePointage;
import com.seneau.offline_sync_service.repository.OfflinePointageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SyncService {
    private final OfflinePointageRepository repository;
    private final SyncPublisher publisher;

    @Transactional
    public OfflinePointage traiterBatch(PointageBatchRequestDto dto) {
        OfflinePointage entity = OfflinePointage.fromDto(dto);
        entity.setStatut(EStatut.PENDING);
        repository.save(entity);

        try {
            publisher.publierBatch(dto);
            entity.setStatut(EStatut.SENT);
            entity.setPublishedAt(LocalDateTime.now());
            entity.setErrorMessage(null);
            repository.save(entity);

        } catch (Exception e) {
            entity.setStatut(EStatut.FAILED);
            entity.setErrorMessage(e.getMessage());
            repository.save(entity);
        }

        return entity;
    }

    @Scheduled(fixedDelay = 600000)
    @Transactional
    public void retryFailedBatches() {
        LocalDateTime limite = LocalDateTime.now().minusMinutes(5);
        List<OfflinePointage> failedList = repository.findByStatutAndPublishedAtBefore(EStatut.FAILED, limite);

        if (failedList.isEmpty()) {
            log.debug("Aucun batch FAILED à republier pour l’instant.");
            return;
        }

        log.info("Début de la republication automatique ({}) batchs échoués.", failedList.size());

        for (OfflinePointage failed : failedList) {
            try {
                PointageBatchRequestDto dto = failed.toDto();
                publisher.publierBatch(dto);

                failed.setStatut(EStatut.SENT);
                failed.setPublishedAt(LocalDateTime.now());
                failed.setErrorMessage(null);
                repository.save(failed);

                log.info("Republier batch ID={} réussi.", failed.getId());
            } catch (Exception e) {
                log.error("Échec de la republication batch ID={} : {}", failed.getId(), e.getMessage());
                failed.setErrorMessage(e.getMessage());
                repository.save(failed);
            }
        }

        log.info("🔁 Fin de la republication automatique.");
    }
}
