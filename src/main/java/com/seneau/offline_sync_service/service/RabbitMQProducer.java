package com.seneau.offline_sync_service.service;

import com.seneau.offline_sync_service.config.RabbitConfig;
import com.seneau.offline_sync_service.web.dto.SyncMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RabbitMQProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Envoyer un message de synchronisation dans la queue
     */
    public void sendSyncMessage(SyncMessageDto message) {
        try {
            log.info("Envoi du batch {} vers RabbitMQ avec {} pointages", 
                    message.getBatchId(), 
                    message.getPointages().size());
            
            rabbitTemplate.convertAndSend(
                    RabbitConfig.POINTAGE_EXCHANGE,
                    RabbitConfig.POINTAGE_ROUTING_KEY,
                    message
            );
            
            log.info("Batch {} envoyé avec succès à RabbitMQ", message.getBatchId());
            
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi du batch {} vers RabbitMQ", 
                    message.getBatchId(), e);
            throw new RuntimeException("Impossible d'envoyer le message à RabbitMQ", e);
        }
    }

    /**
     * Envoyer un message avec priorité
     */
    public void sendSyncMessageWithPriority(SyncMessageDto message, int priority) {
        try {
            log.info("Envoi du batch {} vers RabbitMQ avec priorité {}", 
                    message.getBatchId(), priority);
            
            rabbitTemplate.convertAndSend(
                    RabbitConfig.POINTAGE_EXCHANGE,
                    RabbitConfig.POINTAGE_ROUTING_KEY,
                    message,
                    m -> {
                        m.getMessageProperties().setPriority(priority);
                        return m;
                    }
            );
            
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi prioritaire du batch {}", 
                    message.getBatchId(), e);
            throw new RuntimeException("Impossible d'envoyer le message prioritaire", e);
        }
    }
}