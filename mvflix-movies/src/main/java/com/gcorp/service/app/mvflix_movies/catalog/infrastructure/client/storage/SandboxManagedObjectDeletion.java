package com.gcorp.service.app.mvflix_movies.catalog.infrastructure.client.storage;

import com.gcorp.service.app.mvflix_movies.catalog.application.port.ManagedObjectDeletion;
import com.gcorp.service.app.mvflix_movies.catalog.application.port.ManagedObjectReference;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

/** Implementación local para tests sandbox, donde Storage no está desplegado. */
@Component
@Profile("sandbox")
public class SandboxManagedObjectDeletion implements ManagedObjectDeletion {

    @Override
    public Mono<Void> delete(ManagedObjectReference reference) {
        return Mono.empty();
    }
}
