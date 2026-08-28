package com.gcorp.service.app.mvflix_movies.catalog.infrastructure.scheduler;

import com.gcorp.service.app.mvflix_movies.catalog.application.ManagedMediaDeletionCoordinator;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Adapter scheduler: solo selecciona pendientes y dispara el proceso de
 * aplicación. Cada película tiene su propio aislamiento de errores para que
 * un Storage caído no cancele el resto del lote.
 */
@Slf4j
@Component
@ConditionalOnProperty(
        name = "mvflix.messaging.kafka.enabled",
        havingValue = "false",
        matchIfMissing = true)
public class PendingMediaDeletionJob {

    private final MovieRepository movieRepository;
    private final ManagedMediaDeletionCoordinator coordinator;
    private final int batchSize;
    private final int concurrency;

    public PendingMediaDeletionJob(
            MovieRepository movieRepository,
            ManagedMediaDeletionCoordinator coordinator,
            @Value("${movies.deletion.batch-size:25}") int batchSize,
            @Value("${movies.deletion.concurrency:4}") int concurrency) {
        this.movieRepository = movieRepository;
        this.coordinator = coordinator;
        this.batchSize = batchSize;
        this.concurrency = concurrency;
    }

    @Scheduled(fixedDelayString = "${movies.deletion.retry-delay:PT1M}")
    public void retryPending() {
        retryPendingBatch().subscribe(
                ignored -> {},
                error -> log.error("Error leyendo lote de borrado pendiente", error));
    }

    /** Expuesto como publisher para probar el contrato del adapter sin esperar el scheduler. */
    public Mono<Void> retryPendingBatch() {
        return this.movieRepository
                .findDeleting(this.batchSize)
                .flatMap(movie -> this.coordinator.process(movie.getId())
                        .doOnError(error -> log.warn(
                                "Reintento de borrado fallido para movie={}: {}",
                                movie.getId().value(), error.getMessage()))
                        .onErrorResume(error -> Mono.empty()), this.concurrency)
                .then();
    }
}
