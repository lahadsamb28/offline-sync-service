package com.seneau.offline_sync_service.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class RabbitConfig {
    public static final String EXCHANGE = "offline.pointage.exchange";
    public static final String QUEUE = "offline.pointage.queue";
    public static final String ROUTING_KEY = "offline.pointage.key";

    public static final String DLX = "offline.pointage.dlx";
    public static final String DLQ = "offline.pointage.dlq";
    public static final String DLQ_ROUTING_KEY = "offline.pointage.dlq.key";

    @Bean
    public DirectExchange offlineExchange() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean
    public DirectExchange offlineDlx() {
        return new DirectExchange(DLX);
    }

    @Bean
    public Queue offlineQueue() {
        return QueueBuilder.durable(QUEUE)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .withArgument("x-message-ttl", 60000) // 60s avant d'aller en DLQ
                .build();
    }

    @Bean
    public Queue offlineDlq() {
        return QueueBuilder.durable(DLQ).build();
    }


    @Bean
    public Binding offlineBinding(Queue offlineQueue, DirectExchange offlineExchange) {
        return BindingBuilder.bind(offlineQueue).to(offlineExchange).with(ROUTING_KEY);
    }

    @Bean
    public Binding offlineDlqBinding(Queue offlineDlq, DirectExchange offlineDlx) {
        return BindingBuilder.bind(offlineDlq).to(offlineDlx).with(DLQ_ROUTING_KEY);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMandatory(true);

        // Callback si ACK/NACK côté broker
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                System.err.println("Message non confirmé : " + cause);
                log.error("Message non confirmé : {}", cause);
            }
        });

        // Callback si retour du broker (message non routé)
        template.setReturnsCallback(returned -> {
            System.err.println("Message non routé : " + returned.getMessage());
         log.error("Message non routé : {}", returned.getMessage());
         }
        );

        return template;
    }
}

