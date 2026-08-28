package com.guille.media.reproductor.uploader.storage.managedstorage.domain.port;

import java.util.UUID;

import reactor.core.publisher.Mono;

public interface DeletionInboxRepository {
  Mono<Boolean> isCompleted(UUID eventId);

  Mono<Void> recordReceived(UUID eventId);

  Mono<Void> markCompleted(UUID eventId);

  Mono<Void> markFailed(UUID eventId, String error);
}
