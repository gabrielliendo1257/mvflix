package com.gcorp.service.app.mvflix_movies.catalog.infrastructure.client.kafka;

import com.gcorp.service.app.mvflix_movies.catalog.application.StoredObjectDeletedConsumer;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@ConditionalOnProperty(name = "mvflix.messaging.kafka.enabled", havingValue = "true")
@Slf4j
public class KafkaStoredObjectDeletedListener {

    private final StoredObjectDeletedConsumer consumer;
    private final Counter failures;
    private final Counter dlqMessages;

    public KafkaStoredObjectDeletedListener(
            StoredObjectDeletedConsumer consumer, MeterRegistry meterRegistry) {
        this.consumer = consumer;
        this.failures = meterRegistry.counter("mvflix_kafka_consumer_failures_total", "service", "movies");
        this.dlqMessages = meterRegistry.counter("mvflix_kafka_dlq_messages_total", "service", "movies");
    }

    @RetryableTopic(
            attempts = "4",
            backoff = @org.springframework.retry.annotation.Backoff(delay = 1000, multiplier = 2.0, maxDelay = 10000),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            retryTopicSuffix = ".retry",
            dltTopicSuffix = ".DLT")
    @KafkaListener(
            topics = "${movies.kafka.stored-object-deleted-topic:mvflix.stored-object-deleted.v1}",
            groupId = "${movies.kafka.consumer-group:mvflix-movies}")
    public void onMessage(String payload) {
        try {
            this.consumer.consume(payload).block();
        } catch (RuntimeException error) {
            this.failures.increment();
            throw error;
        }
    }

    @DltHandler
    public void onDlt(String payload) {
        this.dlqMessages.increment();
        log.error("Stored object deletion event moved to DLT: payload={}", payload);
    }
}
