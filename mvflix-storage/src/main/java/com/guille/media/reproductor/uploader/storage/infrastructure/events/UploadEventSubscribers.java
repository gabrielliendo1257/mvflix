package com.guille.media.reproductor.uploader.storage.infrastructure.events;

import com.guille.media.reproductor.uploader.storage.app.user.UserServiceCommandPort;
import com.guille.media.reproductor.uploader.storage.domain.events.UploadCompletedEvent;
import com.guille.media.reproductor.uploader.storage.domain.events.UploadFailedEvent;
import com.guille.media.reproductor.uploader.storage.domain.events.UploadEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Suscriptores de los eventos de upload dentro del storage-service.
 *
 * <p>Destinatarios de los eventos:
 *
 * <ul>
 *   <li>{@code UploadCompletedEvent}: el user-service (validación/registro de la cuota
 *       consumida mediante el puerto {@link UserServiceCommandPort}).
 *   <li>{@code UploadFailedEvent}: el propio cliente mediante {@code GET /upload/{id}}
 *       (el estado queda {@code FAILED}); aquí solo se registra el fallo por si un
 *       consumidor futuro (notificaciones) quiere reaccionar.
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UploadEventSubscribers {

  private final UserServiceCommandPort userServiceCommandPort;

  @EventListener
  void onUploadCompleted(UploadCompletedEvent event) {
    log.info(
        "Upload completed, notifying user-service: subject={}, objectKey={}",
        event.ownerUsername(),
        event.objectKey());
    this.userServiceCommandPort.applyQuota(event.ownerUsername(), event.contentLength());
  }

  @EventListener
  void onUploadFailed(UploadFailedEvent event) {
    log.warn(
        "Upload failed, user notified via status: subject={}, objectKey={}, reason={}",
        event.ownerUsername(),
        event.objectKey(),
        event.reason());
  }
}
