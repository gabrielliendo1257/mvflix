package com.gcorp.service.app.mvflix_movies.catalog.infrastructure.scheduler;

import com.gcorp.service.app.mvflix_movies.catalog.application.port.OutboxMessagePublisher;
import com.gcorp.service.app.mvflix_movies.catalog.application.port.OutboxRepository;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

/** Poller de infraestructura: reclama, publica y confirma mensajes de outbox. */
@Slf4j
@Component
@ConditionalOnProperty(name = "mvflix.messaging.kafka.enabled", havingValue = "true")
public class OutboxPublisherJob {

    private final OutboxRepository outboxRepository;
    private final OutboxMessagePublisher publisher;
    private final int batchSize;
    private final int maxAttempts;
    private final int concurrency;
    private final Duration retryDelay;
    private final Duration lease;
    private final AtomicLong backlog = new AtomicLong();
    private final Counter published;
    private final Counter failed;

    public OutboxPublisherJob(
            OutboxRepository outboxRepository,
            OutboxMessagePublisher publisher,
            MeterRegistry meterRegistry,
            @Value("${movies.outbox.batch-size:25}") int batchSize,
            @Value("${movies.outbox.max-attempts:10}") int maxAttempts,
            @Value("${movies.outbox.concurrency:4}") int concurrency,
            @Value("${movies.outbox.retry-delay:PT1M}") Duration retryDelay,
            @Value("${movies.outbox.lease:PT2M}") Duration lease) {
        this.outboxRepository = outboxRepository;
        this.publisher = publisher;
        this.batchSize = batchSize;
        this.maxAttempts = maxAttempts;
        this.concurrency = concurrency;
        this.retryDelay = retryDelay;
        this.lease = lease;
        meterRegistry.gauge("movies.outbox.backlog", this.backlog);
        this.published = meterRegistry.counter("movies.outbox.published");
        this.failed = meterRegistry.counter("movies.outbox.failed");
    }

    @Scheduled(fixedDelayString = "${movies.outbox.poll-interval:PT5S}")
    public void publishPending() {
        publishPendingBatch().subscribe(
                ignored -> {},
                error -> log.error("Error reclamando outbox", error));
    }

    public Mono<Void> publishPendingBatch() {
        return this.outboxRepository
                .claim(this.batchSize, this.maxAttempts, this.lease)
                .flatMap(message -> this.publisher.publish(message)
                        .then(Mono.defer(() -> this.outboxRepository.markPublished(message.eventId())))
                        .doOnSuccess(ignored -> this.published.increment())
                        .onErrorResume(error -> {
                            this.failed.increment();
                            log.warn("Publicación outbox fallida eventId={}: {}",
                                    message.eventId(), error.getMessage());
                            return Mono.defer(() -> this.outboxRepository
                                    .markFailed(message.eventId(), error.getMessage(), this.retryDelay))
                                    .onErrorResume(markError -> {
                                        log.error("No se pudo registrar fallo outbox eventId={}",
                                                message.eventId(), markError);
                                        return Mono.empty();
                                    });
                        }), this.concurrency)
                .then(this.refreshBacklog());
    }

    private Mono<Void> refreshBacklog() {
        return this.outboxRepository.pendingCount(this.maxAttempts)
                .doOnNext(this.backlog::set)
                .then();
    }
}
