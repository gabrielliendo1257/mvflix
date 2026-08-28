package com.gcorp.service.app.mvflix_movies.catalog.application.port;

import java.util.UUID;

/** Registro de outbox reclamado para publicación; el payload ya es un envelope JSON. */
public record OutboxMessage(
        UUID eventId,
        String eventType,
        int eventVersion,
        String aggregateId,
        String payload) {}
