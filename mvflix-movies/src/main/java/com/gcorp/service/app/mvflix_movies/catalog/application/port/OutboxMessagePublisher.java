package com.gcorp.service.app.mvflix_movies.catalog.application.port;

import reactor.core.publisher.Mono;

/** Capacidad de publicar un registro de outbox en el broker. */
public interface OutboxMessagePublisher {

    Mono<Void> publish(OutboxMessage message);
}
