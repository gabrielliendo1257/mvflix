package com.gcorp.service.app.mvflix_movies.catalog.infrastructure.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gcorp.service.app.mvflix_movies.catalog.application.port.OutboxMessage;
import com.gcorp.service.app.mvflix_movies.catalog.application.port.OutboxMessagePublisher;
import com.gcorp.service.app.mvflix_movies.catalog.application.port.OutboxRepository;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherJobTest {

    @Mock private OutboxRepository outboxRepository;
    @Mock private OutboxMessagePublisher publisher;

    @Test
    void claimsPublishesMarksPublishedAndRefreshesBacklog() {
        OutboxMessage message = message();
        when(this.outboxRepository.claim(25, 10, Duration.ofMinutes(2)))
                .thenReturn(Flux.just(message));
        when(this.publisher.publish(message)).thenReturn(Mono.empty());
        when(this.outboxRepository.markPublished(message.eventId())).thenReturn(Mono.empty());
        when(this.outboxRepository.pendingCount(10)).thenReturn(Mono.just(3L));
        SimpleMeterRegistry registry = new SimpleMeterRegistry();

        new OutboxPublisherJob(this.outboxRepository, this.publisher, registry,
                25, 10, 4, Duration.ofMinutes(1), Duration.ofMinutes(2))
                .publishPendingBatch().block();

        verify(this.publisher).publish(message);
        verify(this.outboxRepository).markPublished(message.eventId());
        assertThat(registry.get("movies.outbox.backlog").gauge().value()).isEqualTo(3.0);
        assertThat(registry.get("movies.outbox.published").counter().count()).isEqualTo(1.0);
    }

    @Test
    void publishFailureIsRecordedAndDoesNotEscapeTheBatch() {
        OutboxMessage message = message();
        RuntimeException failure = new RuntimeException("Kafka down");
        when(this.outboxRepository.claim(25, 10, Duration.ofMinutes(2)))
                .thenReturn(Flux.just(message));
        when(this.publisher.publish(message)).thenReturn(Mono.error(failure));
        when(this.outboxRepository.markFailed(message.eventId(), "Kafka down", Duration.ofMinutes(1)))
                .thenReturn(Mono.empty());
        when(this.outboxRepository.pendingCount(10)).thenReturn(Mono.just(1L));
        SimpleMeterRegistry registry = new SimpleMeterRegistry();

        new OutboxPublisherJob(this.outboxRepository, this.publisher, registry,
                25, 10, 4, Duration.ofMinutes(1), Duration.ofMinutes(2))
                .publishPendingBatch().block();

        verify(this.outboxRepository).markFailed(message.eventId(), "Kafka down", Duration.ofMinutes(1));
        verify(this.outboxRepository, never()).markPublished(any());
        assertThat(registry.get("movies.outbox.failed").counter().count()).isEqualTo(1.0);
    }

    private static OutboxMessage message() {
        return new OutboxMessage(UUID.randomUUID(), "ManagedMediaDeletionRequested", 1,
                "7", "{\"eventId\":\"test\"}");
    }
}
