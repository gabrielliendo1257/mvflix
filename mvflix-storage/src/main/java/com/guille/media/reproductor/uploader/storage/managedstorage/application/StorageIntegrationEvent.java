package com.guille.media.reproductor.uploader.storage.managedstorage.application;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record StorageIntegrationEvent(
    UUID eventId,
    String eventType,
    int eventVersion,
    Instant occurredAt,
    String aggregateId,
    Map<String, Object> payload) {}
