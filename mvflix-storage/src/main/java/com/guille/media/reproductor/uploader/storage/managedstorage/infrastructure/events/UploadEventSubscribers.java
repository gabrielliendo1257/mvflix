package com.guille.media.reproductor.uploader.storage.managedstorage.infrastructure.events;

import com.guille.media.reproductor.uploader.storage.managedstorage.domain.event.UploadCompletedEvent;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.event.UploadFailedEvent;

import lombok.extern.slf4j.Slf4j;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Suscriptores de los eventos de upload dentro del storage-service.
 *
 * <p>El storage es la fuente de verdad del consumo de cuota (reserva, liberación y registro
 * atómico viven en su propia tabla {@code user_storage}); por eso ya no notifica consumo a
 * ningún otro servicio. Aquí solo se registra el evento por si un consumidor futuro
 * (notificaciones, auditoría) quiere reaccionar.
 */
@Slf4j
@Component
public class UploadEventSubscribers {

  @EventListener
  void onUploadCompleted(UploadCompletedEvent event) {
    log.info(
        "Upload completed: subject={}, objectKey={}, size={}",
        event.ownerUsername(),
        event.objectKey(),
        event.contentLength());
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
