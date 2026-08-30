package com.gcorp.service.app.mvflix_media_ingestion.infrastructure.messaging;

import com.gcorp.service.app.mvflix_media_ingestion.application.MediaIngestionRepository;
import com.gcorp.service.app.mvflix_media_ingestion.application.RecoveryService;
import com.gcorp.service.app.mvflix_media_ingestion.domain.MediaIngestion;
import java.time.Duration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    name = "mvflix.recovery.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class RecoveryScheduler {
  private final MediaIngestionRepository repository;
  private final RecoveryService service;

  public RecoveryScheduler(MediaIngestionRepository repository, RecoveryService service) {
    this.repository = repository;
    this.service = service;
  }

  @Scheduled(fixedDelayString = "${mvflix.recovery.poll-ms:5000}")
  public void run() {
    var claimed = repository.claimDueRecoverable(20, Duration.ofMinutes(1));
    (claimed == null ? reactor.core.publisher.Flux.<MediaIngestion>empty() : claimed)
        .flatMap(
            i -> service.recover(i).onErrorResume(error -> service.rescheduleAfterError(i, error)))
        .onErrorResume(error -> reactor.core.publisher.Mono.empty())
        .subscribe();
  }
}
