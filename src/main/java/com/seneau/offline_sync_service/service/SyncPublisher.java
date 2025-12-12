package com.seneau.offline_sync_service.service;

import com.seneau.offline_sync_service.config.RabbitConfig;
import com.seneau.offline_sync_service.dto.PointageBatchRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SyncPublisher {
    private final RabbitTemplate rabbitTemplate;
    private final PointageMetricsService metrics;


    public void publierBatch(PointageBatchRequestDto dto) {
        metrics.incrementReceived();

        try {
            metrics.recordPublishTime(() -> {
                rabbitTemplate.convertAndSend(
                        RabbitConfig.EXCHANGE,
                        RabbitConfig.ROUTING_KEY,
                        dto
                );
                return null;
            });

            metrics.incrementPublished();
            log.info("Batch publié avec succès");
        } catch (Exception e) {
            metrics.incrementFailed();
            log.error("Erreur publication batch : {}", e.getMessage());
            throw e; // On laisse l'exception pour que le service logique mette le statut FAILED
        }
    }
}
