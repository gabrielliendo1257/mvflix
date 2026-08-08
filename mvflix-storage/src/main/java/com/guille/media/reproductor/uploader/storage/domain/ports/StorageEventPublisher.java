package com.guille.media.reproductor.uploader.storage.domain.ports;

import com.guille.media.reproductor.uploader.storage.domain.events.UploadCompletedEvent;

/**
 * Publica eventos de dominio hacia los consumidores (catálogo, auditoría, etc.).
 * El fallo de publicación nunca debe abortar el flujo principal del upload.
 */
public interface StorageEventPublisher {

    void publish(UploadCompletedEvent event);
}