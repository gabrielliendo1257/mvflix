package com.gcorp.service.app.mvflix_movies.catalog.infrastructure.client.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcorp.service.app.mvflix_movies.catalog.application.StoredObjectDeletedConsumer;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
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
    private final ObjectMapper objectMapper;
    private final Counter failures;
    private final Counter dlqMessages;

    public KafkaStoredObjectDeletedListener(
            StoredObjectDeletedConsumer consumer, ObjectMapper objectMapper, MeterRegistry meterRegistry) {
        this.consumer = consumer;
        this.objectMapper = objectMapper;
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
    public void onDlt(ConsumerRecord<?, ?> record, Exception cause) {
        this.dlqMessages.increment();
        log.error("Stored object deletion event moved to DLT: topic={}, key={}, eventId={}, offset={}, cause={}",
                record.topic(), record.key(), eventId(record.value()), record.offset(), cause.getClass().getSimpleName());
    }

    private String eventId(Object value) {
        try {
            JsonNode root = this.objectMapper.readTree(String.valueOf(value));
            JsonNode eventId = root.path("eventId");
            return eventId.isTextual() && !eventId.textValue().isBlank() ? eventId.textValue() : "<unknown>";
        } catch (Exception ignored) {
            return "<invalid>";
        }
    }
}
