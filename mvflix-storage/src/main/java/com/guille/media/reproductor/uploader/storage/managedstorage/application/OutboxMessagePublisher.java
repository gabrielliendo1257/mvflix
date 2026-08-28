package com.guille.media.reproductor.uploader.storage.managedstorage.application;

import reactor.core.publisher.Mono;

public interface OutboxMessagePublisher {
  Mono<Void> publish(StorageOutboxMessage message);
}
