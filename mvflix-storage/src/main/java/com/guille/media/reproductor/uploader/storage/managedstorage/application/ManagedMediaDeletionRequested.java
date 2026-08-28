package com.guille.media.reproductor.uploader.storage.managedstorage.application;

import java.time.Instant;
import java.util.UUID;

public record ManagedMediaDeletionRequested(
    UUID eventId,
    Instant occurredAt,
    long movieId,
    long storageId,
    String ownerUsername,
    String objectKey) {}
