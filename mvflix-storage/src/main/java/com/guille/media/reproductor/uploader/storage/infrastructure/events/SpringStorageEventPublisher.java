package com.guille.media.reproductor.uploader.storage.infrastructure.events;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.guille.media.reproductor.uploader.storage.domain.events.UploadCompletedEvent;
import com.guille.media.reproductor.uploader.storage.domain.ports.StorageEventPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Adaptador de {@link StorageEventPublisher} sobre el {@link ApplicationEventPublisher}
 * de Spring. Un fallo del listener nunca propaga: se registra y se sigue con el flujo.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpringStorageEventPublisher implements StorageEventPublisher {

  private final ApplicationEventPublisher applicationEventPublisher;

  @Override
  public void publish(UploadCompletedEvent event) {
    try {
      this.applicationEventPublisher.publishEvent(event);
      log.info("Published upload completed event: storageId={}", event.storageId());
    } catch (RuntimeException error) {
      log.error("Failed to publish uploadCompleted event: storageId={}", event.storageId(), error);
    }
  }
}