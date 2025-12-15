package com.seneau.offline_sync_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    // Noms des queues et exchanges
    public static final String POINTAGE_EXCHANGE = "pointage.sync.exchange";
    public static final String POINTAGE_QUEUE = "pointage.offline.queue";
    public static final String POINTAGE_ROUTING_KEY = "pointage.offline.routing";

    // Dead Letter Queue pour les messages en échec
    public static final String DLQ_EXCHANGE = "pointage.sync.dlq.exchange";
    public static final String DLQ_QUEUE = "pointage.offline.dlq.queue";
    public static final String DLQ_ROUTING_KEY = "pointage.offline.dlq.routing";

    /**
     * Exchange principal pour les pointages
     */
    @Bean
    public DirectExchange pointageExchange() {
        return new DirectExchange(POINTAGE_EXCHANGE, true, false);
    }

    /**
     * Queue principale pour les pointages offline
     * Avec configuration DLQ
     */
    @Bean
    public Queue pointageQueue() {
        return QueueBuilder.durable(POINTAGE_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .withArgument("x-message-ttl", 3600000) // 1 heure
                .build();
    }

    /**
     * Binding entre exchange et queue
     */
    @Bean
    public Binding pointageBinding(Queue pointageQueue, DirectExchange pointageExchange) {
        return BindingBuilder
                .bind(pointageQueue)
                .to(pointageExchange)
                .with(POINTAGE_ROUTING_KEY);
    }

    /**
     * Dead Letter Exchange
     */
    @Bean
    public DirectExchange dlqExchange() {
        return new DirectExchange(DLQ_EXCHANGE, true, false);
    }

    /**
     * Dead Letter Queue
     */
    @Bean
    public Queue dlqQueue() {
        return QueueBuilder.durable(DLQ_QUEUE).build();
    }

    /**
     * Binding pour DLQ
     */
    @Bean
    public Binding dlqBinding(Queue dlqQueue, DirectExchange dlqExchange) {
        return BindingBuilder
                .bind(dlqQueue)
                .to(dlqExchange)
                .with(DLQ_ROUTING_KEY);
    }

    /**
     * Converter JSON pour sérialiser/désérialiser les messages
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate configuré avec le converter JSON
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    /**
     * Container Factory pour les listeners
     * Configuration avec retry et concurrence
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setConcurrentConsumers(3); // 3 consumers en parallèle
        factory.setMaxConcurrentConsumers(10);
        factory.setPrefetchCount(5); // Préfetch 5 messages
        factory.setDefaultRequeueRejected(false); // Pas de requeue, envoyer au DLQ
        return factory;
    }
}
