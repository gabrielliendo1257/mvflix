package com.gcorp.service.app.mvflix_movies.catalog.infrastructure.scheduler;

import com.gcorp.service.app.mvflix_movies.catalog.application.MovieDeletionTransaction;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItemRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

import java.time.Duration;

/** Recovers managed deletions that predate Kafka or exhausted delivery retries. */
@Slf4j
@Component
@ConditionalOnProperty(name = "mvflix.messaging.kafka.enabled", havingValue = "true")
public class DeletionRecoveryJob {

    private final CatalogItemRepository movieRepository;
    private final MovieDeletionTransaction deletionTransaction;
    private final int batchSize;
    private final Duration retryCooldown;

    public DeletionRecoveryJob(
            CatalogItemRepository movieRepository,
            MovieDeletionTransaction deletionTransaction,
            @Value("${movies.deletion.recovery-batch-size:25}") int batchSize,
            @Value("${movies.deletion.recovery-cooldown:PT1M}") Duration retryCooldown) {
        this.movieRepository = movieRepository;
        this.deletionTransaction = deletionTransaction;
        this.batchSize = batchSize;
        this.retryCooldown = retryCooldown;
    }

    @Scheduled(fixedDelayString = "${movies.deletion.recovery-interval:PT1M}")
    public void recover() {
        recoverBatch().subscribe(
                ignored -> {},
                error -> log.error("Error recuperando borrados gestionados", error));
    }

    public Mono<Void> recoverBatch() {
        return this.movieRepository.findDeletingForRecovery(this.batchSize, this.retryCooldown)
                .flatMap(movie -> this.movieRepository.markRecoveryAttempt(movie.getId())
                        .then(this.deletionTransaction.ensureDeletionRequested(movie.getId()))
                        .doOnError(error -> log.warn(
                                "Recuperación de borrado fallida para movie={}: {}",
                                movie.getId().value(), error.getMessage()))
                        .onErrorResume(error -> Mono.empty()))
                .then();
    }
}
