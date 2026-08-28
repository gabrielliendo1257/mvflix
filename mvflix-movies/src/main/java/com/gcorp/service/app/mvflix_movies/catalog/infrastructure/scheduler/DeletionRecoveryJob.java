package com.gcorp.service.app.mvflix_movies.catalog.infrastructure.scheduler;

import com.gcorp.service.app.mvflix_movies.catalog.application.MovieDeletionTransaction;
import com.gcorp.service.app.mvflix_movies.catalog.application.port.ManagedDeletionOutbox;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieRepository;

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

    private final MovieRepository movieRepository;
    private final MovieDeletionTransaction deletionTransaction;
    private final ManagedDeletionOutbox outbox;
    private final int batchSize;
    private final int maxAttempts;
    private final Duration retryCooldown;

    public DeletionRecoveryJob(
            MovieRepository movieRepository,
            MovieDeletionTransaction deletionTransaction,
            ManagedDeletionOutbox outbox,
            @Value("${movies.deletion.recovery-batch-size:25}") int batchSize,
            @Value("${movies.outbox.max-attempts:10}") int maxAttempts,
            @Value("${movies.deletion.recovery-cooldown:PT1M}") Duration retryCooldown) {
        this.movieRepository = movieRepository;
        this.deletionTransaction = deletionTransaction;
        this.outbox = outbox;
        this.batchSize = batchSize;
        this.maxAttempts = maxAttempts;
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
                        .then(this.outbox.reactivateExhausted(
                                Long.toString(movie.getId().value()), this.maxAttempts))
                        .doOnError(error -> log.warn(
                                "Recuperación de borrado fallida para movie={}: {}",
                                movie.getId().value(), error.getMessage()))
                        .onErrorResume(error -> Mono.empty()))
                .then();
    }
}
