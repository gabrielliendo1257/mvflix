package com.gcorp.service.app.mvflix_media_ingestion.infrastructure.messaging;

import com.gcorp.service.app.mvflix_media_ingestion.application.Outbox.Message;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class KafkaOutboxPublisher {
  private final KafkaTemplate<String, String> kafka;

  public KafkaOutboxPublisher(KafkaTemplate<String, String> kafka) { this.kafka = kafka; }

  public Mono<Void> publish(Message message) {
    String topic = switch (message.eventType()) {
      case "MediaIngestionStarted" -> "mvflix.media-ingestion.started.v1";
      case "MediaIngestionCompleted" -> "mvflix.media-ingestion.completed.v1";
      case "MediaIngestionFailed" -> "mvflix.media-ingestion.failed.v1";
      case "MediaIngestionCancelled" -> "mvflix.media-ingestion.cancelled.v1";
      default -> throw new IllegalArgumentException("Unknown media ingestion event type: " + message.eventType());
    };
    return Mono.fromFuture(() -> kafka.send(topic, message.aggregateId().toString(), message.payload())).then();
  }
}
