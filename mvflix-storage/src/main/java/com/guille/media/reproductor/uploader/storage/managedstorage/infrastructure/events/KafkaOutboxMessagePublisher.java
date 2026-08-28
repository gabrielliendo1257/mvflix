package com.guille.media.reproductor.uploader.storage.managedstorage.infrastructure.events;

import com.guille.media.reproductor.uploader.storage.managedstorage.application.StorageOutboxMessage;
import com.guille.media.reproductor.uploader.storage.managedstorage.application.OutboxMessagePublisher;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

@Component
public class KafkaOutboxMessagePublisher implements OutboxMessagePublisher {

  private final KafkaTemplate<String, String> kafkaTemplate;
  private final KafkaTopicResolver topicResolver;

  public KafkaOutboxMessagePublisher(
      KafkaTemplate<String, String> kafkaTemplate, KafkaTopicResolver topicResolver) {
    this.kafkaTemplate = kafkaTemplate;
    this.topicResolver = topicResolver;
  }

  @Override
  public Mono<Void> publish(StorageOutboxMessage message) {
    return Mono.fromFuture(() -> this.kafkaTemplate
        .send(this.topicResolver.resolve(message.eventType()), message.aggregateId(), message.payload())).then();
  }
}
