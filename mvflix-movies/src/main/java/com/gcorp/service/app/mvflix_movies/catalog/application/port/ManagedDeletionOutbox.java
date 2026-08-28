package com.gcorp.service.app.mvflix_movies.catalog.application.port;

import reactor.core.publisher.Mono;

/** Persistencia transaccional del evento que solicita borrar media MANAGED. */
public interface ManagedDeletionOutbox {

    Mono<Void> append(ManagedMediaDeletionRequested event);
}
