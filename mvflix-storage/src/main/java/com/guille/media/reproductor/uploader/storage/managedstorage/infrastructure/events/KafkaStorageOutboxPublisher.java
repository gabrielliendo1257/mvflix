package com.guille.media.reproductor.uploader.storage.managedstorage.infrastructure.events;

import com.guille.media.reproductor.uploader.storage.managedstorage.application.StorageOutboxMessage;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

@Component
public class KafkaStorageOutboxPublisher {

  private static final String TOPIC = "mvflix.stored-object-deleted.v1";

  private final KafkaTemplate<String, String> kafkaTemplate;

  public KafkaStorageOutboxPublisher(KafkaTemplate<String, String> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
  }

  public Mono<Void> publish(StorageOutboxMessage message) {
    return Mono.fromFuture(() -> this.kafkaTemplate
        .send(TOPIC, message.aggregateId(), message.payload())).then();
  }
}
