package com.seneau.offline_sync_service.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
public class PointageMetricsService {

    private final Counter batchReceivedCounter;
    private final Counter batchFailedCounter;
    private final Counter batchPublishedCounter;
    private final Timer publishLatencyTimer;

    public PointageMetricsService(MeterRegistry registry) {

        this.batchReceivedCounter = Counter.builder("offline_batches_total")
                .description("Nombre total de batchs reçus")
                .register(registry);

        this.batchPublishedCounter = Counter.builder("offline_batches_published_total")
                .description("Nombre total de batchs publiés avec succès")
                .register(registry);

        this.batchFailedCounter = Counter.builder("offline_batches_failed_total")
                .description("Nombre total de batchs ayant échoué")
                .register(registry);

        this.publishLatencyTimer = Timer.builder("offline_publish_latency_seconds")
                .description("Durée de publication des batchs vers RabbitMQ")
                .publishPercentiles(0.5, 0.9, 0.99)
                .register(registry);
    }

    public void incrementReceived() {
        batchReceivedCounter.increment();
    }

    public void incrementPublished() {
        batchPublishedCounter.increment();
    }

    public void incrementFailed() {
        batchFailedCounter.increment();
    }

    public <T> T recordPublishTime(java.util.function.Supplier<T> supplier) {
        return publishLatencyTimer.record(supplier);
    }
}
