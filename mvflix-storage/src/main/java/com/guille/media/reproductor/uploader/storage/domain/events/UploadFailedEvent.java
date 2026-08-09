package com.guille.media.reproductor.uploader.storage.domain.events;

import java.time.Instant;

/**
 * Evento de dominio emitido cuando la verificación de una sesión de upload falla
 * (objeto inexistente, tamaño inválido, etc.). El objeto queda en {@code FAILED},
 * la cuota reservada se libera y el consumidor (p. ej. notificaciones al usuario)
 * puede alertar del error.
 */
public record UploadFailedEvent(
    Long storageId,
    String ownerUsername,
    String objectKey,
    String reason,
    Instant occurredAt)
    implements UploadEvent {}
