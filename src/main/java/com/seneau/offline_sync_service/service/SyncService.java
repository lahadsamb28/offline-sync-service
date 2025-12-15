package com.seneau.offline_sync_service.service;

import com.seneau.offline_sync_service.data.model.*;
import com.seneau.offline_sync_service.data.repository.SyncBatchRepository;
import com.seneau.offline_sync_service.web.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SyncService {

    private final RabbitMQProducer rabbitMQProducer;
    private final SyncBatchRepository syncBatchRepository;
    private final ValidationService validationService;

    /**
     * Synchroniser des pointages STANDARDS (avec matricule)
     */
    @Transactional
    public SyncResponseDto syncPointagesStandard(String email, PointageBatchRequestDto request) {
        String batchId = UUID.randomUUID().toString();

        log.info("📦 Création batch STANDARD {} avec {} pointages pour {}",
                 batchId, request.getPointages().size(), email);

        // Validation des données
        List<String> validationErrors = validationService.validateBatchStandard(request);
        if (!validationErrors.isEmpty()) {
            log.warn("⚠️ Erreurs de validation pour le batch {}: {}", batchId, validationErrors);
            return createErrorResponse(batchId, validationErrors);
        }

        try {
            // Créer le batch en base de données
            SyncBatch batch = createBatch(batchId, email, request.getPointages().size(), TypePointageSync.STANDARD);
            syncBatchRepository.save(batch);
            log.info("✅ Batch {} créé en base avec statut PENDING", batchId);

            // Créer le message pour RabbitMQ
            SyncMessageDto message = SyncMessageDto.builder()
                    .batchId(batchId)
                    .email(email)
                    .typePointage(TypePointageSync.STANDARD)
                    .pointages(request.getPointages())
                    .createdAt(LocalDateTime.now())
                    .retryCount(0)
                    .maxRetries(3)
                    .build();

            // Envoyer à RabbitMQ
            rabbitMQProducer.sendSyncMessage(message);
            log.info("🐰 Batch {} envoyé à RabbitMQ avec succès", batchId);

            return SyncResponseDto.builder()
                    .batchId(batchId)
                    .status(SyncStatus.PENDING.name())
                    .totalPointages(request.getPointages().size())
                    .successCount(0)
                    .failureCount(0)
                    .message("Batch STANDARD envoyé à la queue de traitement")
                    .sentToQueue(true)
                    .build();

        } catch (Exception e) {
            log.error("❌ Erreur lors de la création du batch {}", batchId, e);
            return handleBatchCreationError(batchId, request.getPointages().size(), e);
        }
    }

    /**
     * Synchroniser des pointages TERRAIN (avec GPS, sans matricule)
     */
    @Transactional
    public SyncResponseDto syncPointagesTerrain(String email, PointageTerrainBatchRequestDto request, List<String> privileges) {
        String batchId = UUID.randomUUID().toString();

        log.info("📦 Création batch TERRAIN {} avec {} pointages pour {}",
                 batchId, request.getPointages().size(), email);

        // Validation des données
        List<String> validationErrors = validationService.validateBatchTerrain(request);
        if (!validationErrors.isEmpty()) {
            log.warn("⚠️ Erreurs de validation pour le batch {}: {}", batchId, validationErrors);
            return createErrorResponse(batchId, validationErrors);
        }

        try {
            // Créer le batch en base de données
            SyncBatch batch = createBatch(batchId, email, request.getPointages().size(), TypePointageSync.TERRAIN);
            syncBatchRepository.save(batch);
            log.info("✅ Batch {} créé en base avec statut PENDING", batchId);

            // Créer le message pour RabbitMQ
            SyncMessageDto message = SyncMessageDto.builder()
                    .batchId(batchId)
                    .email(email)
                    .typePointage(TypePointageSync.TERRAIN)
                    .pointagesTerrain(request.getPointages())
                    .privileges(privileges)
                    .createdAt(LocalDateTime.now())
                    .retryCount(0)
                    .maxRetries(3)
                    .build();

            // Envoyer à RabbitMQ
            rabbitMQProducer.sendSyncMessage(message);
            log.info("🐰 Batch {} envoyé à RabbitMQ avec succès", batchId);

            return SyncResponseDto.builder()
                    .batchId(batchId)
                    .status(SyncStatus.PENDING.name())
                    .totalPointages(request.getPointages().size())
                    .successCount(0)
                    .failureCount(0)
                    .message("Batch TERRAIN envoyé à la queue de traitement")
                    .sentToQueue(true)
                    .build();

        } catch (Exception e) {
            log.error("❌ Erreur lors de la création du batch {}", batchId, e);
            return handleBatchCreationError(batchId, request.getPointages().size(), e);
        }
    }

    /**
     * Récupérer le statut d'un batch
     */
    public SyncStatusDto getSyncStatus(String batchId) {
        SyncBatch batch = syncBatchRepository.findByBatchId(batchId)
                .orElseThrow(() -> new RuntimeException("Batch non trouvé: " + batchId));

        return SyncStatusDto.builder()
                .batchId(batch.getBatchId())
                .status(batch.getStatus().name())
                .totalPointages(batch.getTotalPointages())
                .successCount(batch.getSuccessCount())
                .failureCount(batch.getFailureCount())
                .createdAt(batch.getCreatedAt())
                .completedAt(batch.getCompletedAt())
                .errorMessage(batch.getErrorMessage())
                .build();
    }

    /**
     * Récupérer tous les batchs en échec pour un utilisateur
     */
    public List<PointageBatchDto> getFailedSyncs(String email) {
        List<SyncBatch> failedBatches = syncBatchRepository
                .findByEmailAndStatusIn(email,
                                        List.of(SyncStatus.FAILED, SyncStatus.PARTIAL_FAILURE));

        return failedBatches.stream()
                .flatMap(batch -> {
                    if (batch.getResults() != null) {
                        return batch.getResults().stream()
                                .filter(result -> "ECHEC".equals(result.getStatut()));
                    }
                    return java.util.stream.Stream.empty();
                })
                .toList();
    }

    /**
     * Réessayer un batch échoué
     */
    @Transactional
    public SyncResponseDto retrySync(String batchId) {
        SyncBatch batch = syncBatchRepository.findByBatchId(batchId)
                .orElseThrow(() -> new RuntimeException("Batch non trouvé: " + batchId));

        // Vérifier le statut
        if (batch.getStatus() != SyncStatus.FAILED &&
                batch.getStatus() != SyncStatus.PARTIAL_FAILURE) {
            throw new RuntimeException("Le batch n'est pas en échec");
        }

        log.info("🔄 Réessai du batch {}", batchId);

        // Renvoyer le message à RabbitMQ
        SyncMessageDto message = SyncMessageDto.builder()
                .batchId(UUID.randomUUID().toString()) // Nouveau batch ID
                .email(batch.getEmail())
                .typePointage(TypePointageSync.STANDARD) // À adapter selon le type du batch
                .retryCount(0)
                .maxRetries(3)
                .build();

        rabbitMQProducer.sendSyncMessage(message);

        return SyncResponseDto.builder()
                .batchId(message.getBatchId())
                .status(SyncStatus.PENDING.name())
                .message("Réessai lancé avec succès")
                .sentToQueue(true)
                .build();
    }

    /**
     * Méthodes utilitaires privées
     */
    private SyncBatch createBatch(String batchId, String email, int totalPointages, TypePointageSync type) {
        return SyncBatch.builder()
                .batchId(batchId)
                .email(email)
                .totalPointages(totalPointages)
                .successCount(0)
                .failureCount(0)
                .status(SyncStatus.PENDING)
                .typePointage(type.name())
                .createdAt(LocalDateTime.now())
                .build();
    }

    private SyncResponseDto createErrorResponse(String batchId, List<String> errors) {
        return SyncResponseDto.builder()
                .batchId(batchId)
                .status(SyncStatus.FAILED.name())
                .failureCount(1)
                .message("Erreurs de validation")
                .validationErrors(errors)
                .sentToQueue(false)
                .build();
    }

    private SyncResponseDto handleBatchCreationError(String batchId, int totalPointages, Exception e) {
        // Mettre à jour le batch en échec si existant
        syncBatchRepository.findByBatchId(batchId).ifPresent(batch -> {
            batch.setStatus(SyncStatus.FAILED);
            batch.setErrorMessage(e.getMessage());
            batch.setCompletedAt(LocalDateTime.now());
            syncBatchRepository.save(batch);
        });

        return SyncResponseDto.builder()
                .batchId(batchId)
                .status(SyncStatus.FAILED.name())
                .totalPointages(totalPointages)
                .message("Erreur lors de l'envoi à la queue: " + e.getMessage())
                .sentToQueue(false)
                .build();
    }
}
