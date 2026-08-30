package com.guille.media.bff.infrastructure.persistence;

import com.guille.media.bff.experience.addmedia.application.port.AddMediaCompensationRepository;
import com.guille.media.bff.experience.addmedia.application.port.AddMediaCompensationRepository.Kind;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;

/** Reports abandoned claims without inventing non-idempotent compensations. */
@Component
@Profile("!local")
@Slf4j
public class AddMediaProcessRecoveryJob {
  private final R2dbcAddMediaProcessRepository repository;
  private final AddMediaCompensationRepository compensations;

  public AddMediaProcessRecoveryJob(R2dbcAddMediaProcessRepository repository,
      AddMediaCompensationRepository compensations) {
    this.repository = repository;
    this.compensations = compensations;
  }

  @Scheduled(fixedDelayString = "${add-media.recovery.interval-ms:60000}")
  public void reportStaleClaims() {
    repository.findStaleClaims(Duration.ofMinutes(5))
        .flatMap(stale -> {
          log.error("add-media claim atascado: process={} phase={} movie={} upload={}", stale.id(),
              stale.phase(), stale.movieId(), stale.uploadId());
          return durableCompensations(stale);
        })
        .onErrorResume(error -> {
          log.warn("No se pudo consultar recuperación Add Media: {}", error.getMessage());
          return reactor.core.publisher.Flux.empty();
        }).subscribe();
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
    // PREPARING without an ID and FINALIZING have no safe command to invent.
    return upload.then(movie);
  }
}
