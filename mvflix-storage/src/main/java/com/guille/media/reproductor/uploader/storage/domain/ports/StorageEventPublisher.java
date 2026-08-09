package com.guille.media.reproductor.uploader.storage.domain.ports;
import com.guille.media.reproductor.uploader.storage.domain.events.UploadEvent;

import com.guille.media.reproductor.uploader.storage.domain.events.UploadCompletedEvent;
import com.guille.media.reproductor.uploader.storage.domain.events.UploadFailedEvent;

/**
 * Publica eventos de dominio hacia los consumidores (catálogo, auditoría, etc.).
 * El fallo de publicación nunca debe abortar el flujo principal del upload.
 */
public interface StorageEventPublisher {

    void publish(UploadEvent event);
}