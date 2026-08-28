package com.guille.media.reproductor.uploader.storage.managedstorage.application;

import java.util.UUID;

public record StorageOutboxMessage(
    UUID eventId,
    String eventType,
    int eventVersion,
    String aggregateId,
    String payload) {}
