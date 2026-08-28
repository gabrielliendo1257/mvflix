package com.guille.media.reproductor.uploader.storage.managedstorage.infrastructure.events;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guille.media.reproductor.uploader.storage.managedstorage.application.ManagedMediaDeletionConsumer;

import lombok.extern.slf4j.Slf4j;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@ConditionalOnProperty(name = "mvflix.messaging.kafka.enabled", havingValue = "true")
public class KafkaManagedMediaDeletionListener {

  private final ManagedMediaDeletionConsumer consumer;
  private final ObjectMapper objectMapper;
  private final MeterRegistry meterRegistry;
  private final Counter failures;
  private final Counter dlqMessages;
  private final Timer duration;

  public KafkaManagedMediaDeletionListener(
      ManagedMediaDeletionConsumer consumer, ObjectMapper objectMapper, MeterRegistry meterRegistry) {
    this.consumer = consumer;
    this.objectMapper = objectMapper;
    this.meterRegistry = meterRegistry;
    this.failures = meterRegistry.counter("mvflix_kafka_consumer_failures_total", "service", "storage");
    this.dlqMessages = meterRegistry.counter("mvflix_kafka_dlq_messages_total", "service", "storage");
    this.duration = meterRegistry.timer("mvflix_managed_deletion_duration", "service", "storage");
  }

  @RetryableTopic(
      attempts = "4",
      backoff = @org.springframework.retry.annotation.Backoff(delay = 1000, multiplier = 2.0, maxDelay = 10000),
      topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
      retryTopicSuffix = ".retry",
      dltTopicSuffix = ".DLT")
  @KafkaListener(
      topics = "${storage.kafka.deletion-topic:mvflix.managed-media-deletion-requested.v1}",
      groupId = "${storage.kafka.consumer-group:mvflix-storage}")
  public void onMessage(String payload) {
    Timer.Sample sample = Timer.start(this.meterRegistry);
    try {
      this.consumer.consume(payload).block();
      sample.stop(this.duration);
    } catch (RuntimeException error) {
      this.failures.increment();
      sample.stop(this.duration);
      throw error;
    }
  }

  @DltHandler
  public void onDlt(ConsumerRecord<?, ?> record, Exception cause) {
    this.dlqMessages.increment();
    log.error("Managed media deletion event moved to DLT: topic={}, key={}, eventId={}, offset={}, cause={}",
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
