package com.seneau.offline_sync_service.service;

import com.seneau.offline_sync_service.config.RabbitConfig;
import com.seneau.offline_sync_service.data.model.SyncBatch;
import com.seneau.offline_sync_service.data.model.SyncStatus;
import com.seneau.offline_sync_service.data.model.TypePointageSync;
import com.seneau.offline_sync_service.data.repository.SyncBatchRepository;
import com.seneau.offline_sync_service.web.client.PointageServiceClient;
import com.seneau.offline_sync_service.web.dto.PointageBatchDto;
import com.seneau.offline_sync_service.web.dto.PointageBatchRequestDto;
import com.seneau.offline_sync_service.web.dto.PointageTerrainBatchRequestDto;
import com.seneau.offline_sync_service.web.dto.SyncMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RabbitMQConsumer {

    private final PointageServiceClient pointageClient;
    private final SyncBatchRepository syncBatchRepository;
    private final RabbitMQProducer rabbitMQProducer;

    /**
     * Consommer les messages de la queue principale
     * Gère les 2 types de pointages: STANDARD et TERRAIN
     */
    @RabbitListener(queues = RabbitConfig.POINTAGE_QUEUE)
    @Transactional
    public void consumeSyncMessage(SyncMessageDto message) {
        String batchId = message.getBatchId();
        TypePointageSync type = message.getTypePointage();

        log.info("📨 Consommation du batch {} de type {}", batchId, type);

        // Récupérer le batch depuis la BDD
        SyncBatch batch = syncBatchRepository.findByBatchId(batchId)
                .orElseThrow(() -> new RuntimeException("Batch non trouvé: " + batchId));

        try {
            // Mettre à jour le statut
            batch.setStatus(SyncStatus.IN_PROGRESS);
            syncBatchRepository.save(batch);

            // Appeler le service approprié selon le type
            List<PointageBatchDto> results;

            if (type == TypePointageSync.STANDARD) {
                results = processBatchStandard(message);
            } else {
                results = processBatchTerrain(message);
            }

            // Analyser les résultats
            analyzeAndUpdateBatch(batch, results);

        } catch (Exception e) {
            log.error("❌ Erreur lors du traitement du batch {}", batchId, e);
            handleError(batch, message, e);
        }
    }

    /**
     * Traiter un batch STANDARD (avec matricules)
     */
    private List<PointageBatchDto> processBatchStandard(SyncMessageDto message) {
        log.info("⚙️ Traitement batch STANDARD avec {} pointages",
                 message.getPointages().size());

        PointageBatchRequestDto request = new PointageBatchRequestDto();
        request.setPointages(message.getPointages());

        return pointageClient.enregistrerPointagesOffline(
                message.getEmail(),
                request
        );
    }

    /**
     * Traiter un batch TERRAIN (avec GPS)
     */
    private List<PointageBatchDto> processBatchTerrain(SyncMessageDto message) {
        log.info("⚙️ Traitement batch TERRAIN avec {} pointages",
                 message.getPointagesTerrain().size());

        PointageTerrainBatchRequestDto request = new PointageTerrainBatchRequestDto();
        request.setPointages(message.getPointagesTerrain());

        return pointageClient.enregistrerPointagesOfflineTerrain(
                message.getEmail(),
                request,
                message.getPrivileges() != null ? message.getPrivileges() : List.of("AUTH_COLLABORATEUR")
        );
    }

    /**
     * Analyser les résultats et mettre à jour le batch
     */
    private void analyzeAndUpdateBatch(SyncBatch batch, List<PointageBatchDto> results) {
        long successCount = results.stream()
                .filter(r -> "SUCCES".equals(r.getStatut()))
                .count();

        long failureCount = results.size() - successCount;

        // Mettre à jour le batch
        batch.setSuccessCount((int) successCount);
        batch.setFailureCount((int) failureCount);
        batch.setResults(results);
        batch.setCompletedAt(LocalDateTime.now());

        if (failureCount == 0) {
            batch.setStatus(SyncStatus.COMPLETED);
            log.info("✅ Batch {} terminé avec succès: {}/{} pointages",
                     batch.getBatchId(), successCount, results.size());
        } else {
            batch.setStatus(SyncStatus.PARTIAL_FAILURE);
            log.warn("⚠️ Batch {} terminé partiellement: {} succès, {} échecs",
                     batch.getBatchId(), successCount, failureCount);
        }

        syncBatchRepository.save(batch);
    }

    /**
     * Gérer les erreurs avec retry
     */
    private void handleError(SyncBatch batch, SyncMessageDto message, Exception e) {
        int retryCount = message.getRetryCount() != null ? message.getRetryCount() : 0;
        Integer maxRetries = message.getMaxRetries() != null ? message.getMaxRetries() : 3;

        batch.setErrorMessage(e.getMessage());

        if (retryCount < maxRetries) {
            // Réessayer
            log.warn("🔄 Échec du batch {}, réessai {}/{}",
                     batch.getBatchId(), retryCount + 1, maxRetries);

            batch.setStatus(SyncStatus.RETRY);
            syncBatchRepository.save(batch);

            // Renvoyer dans la queue avec compteur incrémenté
            message.setRetryCount(retryCount + 1);

            try {
                // Backoff exponentiel: 2s, 4s, 8s
                long backoffMs = (long) (2000 * Math.pow(2, retryCount));
                Thread.sleep(backoffMs);
                rabbitMQProducer.sendSyncMessage(message);
                log.info("🔄 Batch {} renvoyé à la queue après {}ms",
                         batch.getBatchId(), backoffMs);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.error("Interruption du thread de retry", ie);
            }

        } else {
            // Échec définitif
            log.error("❌ Échec définitif du batch {} après {} tentatives",
                      batch.getBatchId(), maxRetries);

            batch.setStatus(SyncStatus.FAILED);
            batch.setCompletedAt(LocalDateTime.now());
            syncBatchRepository.save(batch);
        }
    }

    /**
     * Consommer les messages de la Dead Letter Queue
     * Pour monitoring et alertes
     */
    @RabbitListener(queues = RabbitConfig.DLQ_QUEUE)
    public void consumeDeadLetterMessage(SyncMessageDto message) {
        log.error("💀 Message en Dead Letter Queue - Batch: {}, Type: {}, Email: {}",
                  message.getBatchId(),
                  message.getTypePointage(),
                  message.getEmail());

        // Mettre à jour le batch en base
        syncBatchRepository.findByBatchId(message.getBatchId())
                .ifPresent(batch -> {
                    batch.setStatus(SyncStatus.FAILED);
                    batch.setErrorMessage("Message envoyé au DLQ après échecs multiples");
                    batch.setCompletedAt(LocalDateTime.now());
                    syncBatchRepository.save(batch);

                    log.error("💀 Batch {} marqué comme FAILED dans la BDD", message.getBatchId());
                });

        // TODO: Envoyer une alerte email/Slack/etc.
        sendAlert(message);
    }

    /**
     * Envoyer une alerte pour les messages en DLQ
     */
    private void sendAlert(SyncMessageDto message) {
        // À implémenter selon votre système d'alertes
        log.error("🚨 ALERTE: Batch {} en DLQ - Type: {}, Email: {}, Retry: {}/{}",
                  message.getBatchId(),
                  message.getTypePointage(),
                  message.getEmail(),
                  message.getRetryCount(),
                  message.getMaxRetries());
    }
}