package com.guille.media.reproductor.uploader.storage.domain.events;

import java.time.Instant;

/**
 * Evento de dominio emitido cuando una sesión de upload se valida y el objeto
 * queda disponible ({@code COMPLETED}). Los consumidores (p. ej. el catálogo
 * de películas) pueden reaccionar al alta real del contenido.
 */
public record UploadCompletedEvent(
    Long storageId, String ownerUsername, String objectKey, String contentType, Long contentLength, Instant completedAt) {}