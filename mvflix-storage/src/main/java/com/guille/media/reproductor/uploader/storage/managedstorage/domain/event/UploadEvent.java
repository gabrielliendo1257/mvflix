package com.guille.media.reproductor.uploader.storage.managedstorage.domain.event;

import java.time.Instant;

/**
 * Raíz sellada de los eventos de dominio del ciclo de vida de un upload.
 *
 * <p>Cada variante aporta su contexto propio (datos del objeto, motivo, etc.).
 * Los consumidores se suscriben al tipo concreto ({@code UploadCompletedEvent},
 * {@code UploadFailedEvent}, ...) o al contexto en general ({@code UploadEvent}).
 */
public sealed interface UploadEvent permits UploadCompletedEvent, UploadFailedEvent {

  Long storageId();

  String ownerUsername();

  String objectKey();

  Instant occurredAt();
}
