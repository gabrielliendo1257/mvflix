package com.guille.media.reproductor.uploader.storage.managedstorage.domain.port;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.event.UploadEvent;

import com.guille.media.reproductor.uploader.storage.managedstorage.domain.event.UploadCompletedEvent;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.event.UploadFailedEvent;

/**
 * Publica eventos de dominio hacia los consumidores (catálogo, auditoría, etc.).
 * El fallo de publicación nunca debe abortar el flujo principal del upload.
 */
public interface StorageEventPublisher {

    void publish(UploadEvent event);
}