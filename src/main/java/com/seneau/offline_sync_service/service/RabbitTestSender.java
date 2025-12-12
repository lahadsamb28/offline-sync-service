package com.seneau.offline_sync_service.service;

import com.seneau.offline_sync_service.config.RabbitConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class RabbitTestSender implements CommandLineRunner {
    private final RabbitTemplate rabbitTemplate;

    public RabbitTestSender(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void run(String... args) {
        try {
            System.out.println("🔄 Tentative d'envoi d'un message de test à RabbitMQ...");
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY, "Message de test TabbitMQ");
            System.out.println("✅ Message envoyé avec succès !");
        } catch (Exception e) {
            System.err.println("❌ Erreur de connexion RabbitMQ : " + e.getMessage());
        }
    }
}
