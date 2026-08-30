package com.guille.media.bff.infrastructure.persistence;

import com.guille.media.bff.experience.addmedia.application.port.AddMediaCompensationRepository;
import com.guille.media.bff.experience.addmedia.application.port.AddMediaCompensationRepository.Kind;
import com.guille.media.bff.experience.addmedia.application.port.AddMediaStorage;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;

/**
 * Recovers abandoned PREPARING claims only when compensation is observable.
 * FINALIZING is deliberately reported as {@code RECONCILIATION_REQUIRED}: this
 * module has no atomic cross-service read that can prove READY or safe rollback.
 */
@Component
@Profile("!local")
@Slf4j
public class AddMediaProcessRecoveryJob {
  private final R2dbcAddMediaProcessRepository repository;
  private final AddMediaCompensationRepository compensations;
  private final AddMediaStorage storage;

  public AddMediaProcessRecoveryJob(R2dbcAddMediaProcessRepository repository,
      AddMediaCompensationRepository compensations, AddMediaStorage storage) {
    this.repository = repository;
    this.compensations = compensations;
    this.storage = storage;
  }

  @Scheduled(fixedDelayString = "${add-media.recovery.interval-ms:60000}")
  public void reportStaleClaims() {
    repository.findStaleClaims(Duration.ofMinutes(5))
        .flatMap(stale -> {
           log.error("add-media claim atascado: process={} phase={} movie={} upload={}", stale.id(),
               stale.phase(), stale.movieId(), stale.uploadId());
           if (stale.phase() == com.guille.media.bff.experience.addmedia.model.AddMediaPhase.FINALIZING) {
             log.error("add-media reconciliation required: process={} movie={} upload={}",
                 stale.id(), stale.movieId(), stale.uploadId());
           }
            return recover(stale);
        })
        .onErrorResume(error -> {
          log.warn("No se pudo consultar recuperación Add Media: {}", error.getMessage());
          return reactor.core.publisher.Flux.empty();
        }).subscribe();
  }

  private reactor.core.publisher.Mono<Void> recover(
      R2dbcAddMediaProcessRepository.StaleProcess stale) {
    if (stale.phase() == com.guille.media.bff.experience.addmedia.model.AddMediaPhase.PREPARING
        && stale.movieId() != null && stale.uploadId() == null) {
      if (stale.idempotencyKey() == null) {
        log.warn("add-media PREPARING sin idempotency key, se conserva: process={}", stale.id());
        return reactor.core.publisher.Mono.empty();
      }
      return storage.recoverUpload(stale.ownerSubject(), stale.idempotencyKey())
          .flatMap(session -> {
            Long uploadId = Long.valueOf(session.uploadId());
            return repository.claimRecoveredCancellation(stale.id(), stale.version(), uploadId)
                .then(compensations.enqueue(stale.id(), Kind.CANCEL_UPLOAD, uploadId,
                    new IllegalStateException("recovered stale upload")))
                .then(compensations.enqueue(stale.id(), Kind.DISCARD_DRAFT, stale.movieId(),
                    new IllegalStateException("stale PREPARING")));
          })
          // 404 is a confirmed absence. Other errors keep PREPARING for retry.
          .switchIfEmpty(durableCompensations(stale)
              .then(repository.completePreparingRecovery(stale.id(), true).then()));
    }
    return durableCompensations(stale)
        .then(stale.phase() == com.guille.media.bff.experience.addmedia.model.AddMediaPhase.PREPARING
            ? repository.completePreparingRecovery(stale.id()).then()
            : reactor.core.publisher.Mono.empty());
  }

  private reactor.core.publisher.Mono<Void> durableCompensations(
      R2dbcAddMediaProcessRepository.StaleProcess stale) {
    if (stale.phase() != com.guille.media.bff.experience.addmedia.model.AddMediaPhase.PREPARING
        && stale.phase() != com.guille.media.bff.experience.addmedia.model.AddMediaPhase.CANCELLING) {
      // FINALIZING may already have completed Movies/Storage. Compensation is
      // unsafe until the downstream state has been reconciled explicitly.
      return reactor.core.publisher.Mono.empty();
    }
    reactor.core.publisher.Mono<Void> upload = stale.uploadId() == null
        ? reactor.core.publisher.Mono.empty()
        : compensations.enqueue(stale.id(), Kind.CANCEL_UPLOAD, stale.uploadId(),
            new IllegalStateException("stale " + stale.phase()));
    reactor.core.publisher.Mono<Void> movie = stale.movieId() == null
        ? reactor.core.publisher.Mono.empty()
        : compensations.enqueue(stale.id(), Kind.DISCARD_DRAFT, stale.movieId(),
            new IllegalStateException("stale " + stale.phase()));
    // PREPARING without an ID is handled only after the idempotency lookup above.
    return upload.then(movie);
  }

}
