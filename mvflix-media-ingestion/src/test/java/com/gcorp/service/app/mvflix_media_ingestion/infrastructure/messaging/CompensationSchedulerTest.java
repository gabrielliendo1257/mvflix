package com.gcorp.service.app.mvflix_media_ingestion.infrastructure.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gcorp.service.app.mvflix_media_ingestion.application.CompensationRepository;
import com.gcorp.service.app.mvflix_media_ingestion.application.DownstreamClients;
import com.gcorp.service.app.mvflix_media_ingestion.application.MediaIngestionRepository;
import com.gcorp.service.app.mvflix_media_ingestion.domain.MediaIngestion;
import com.gcorp.service.app.mvflix_media_ingestion.domain.MediaIngestion.Phase;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class CompensationSchedulerTest {
  @Mock CompensationRepository repository;
  @Mock MediaIngestionRepository ingestions;
  @Mock DownstreamClients clients;

  @Test
  void discardsOnlyWhenCatalogIsStillDraft() {
    var ingestion = ingestion();
    var compensation = compensation(ingestion.ingestionId(), "DISCARD_DRAFT");
    when(repository.due(20)).thenReturn(Flux.just(compensation));
    when(ingestions.find(ingestion.ingestionId())).thenReturn(Mono.just(ingestion));
    when(clients.catalogStatus(42L, "pepe"))
        .thenReturn(Mono.just(new DownstreamClients.CatalogStatus("DRAFT")));
    when(clients.discardDraft(42L, "pepe", ingestion.ingestionId() + ":discard-draft"))
        .thenReturn(Mono.empty());
    when(repository.success(compensation.id())).thenReturn(Mono.empty());

    new CompensationScheduler(repository, ingestions, clients).run();

    verify(clients).discardDraft(42L, "pepe", ingestion.ingestionId() + ":discard-draft");
    verify(repository).success(compensation.id());
  }

  @Test
  void readyCatalogIsASuccessfulNoOp() {
    var ingestion = ingestion();
    var compensation = compensation(ingestion.ingestionId(), "DISCARD_DRAFT");
    when(repository.due(20)).thenReturn(Flux.just(compensation));
    when(ingestions.find(ingestion.ingestionId())).thenReturn(Mono.just(ingestion));
    when(clients.catalogStatus(42L, "pepe"))
        .thenReturn(Mono.just(new DownstreamClients.CatalogStatus("READY")));
    when(repository.success(compensation.id())).thenReturn(Mono.empty());

    new CompensationScheduler(repository, ingestions, clients).run();

    verify(clients, never()).discardDraft(any(Long.class), any(), any());
    verify(repository).success(compensation.id());
  }

  private static CompensationRepository.Compensation compensation(UUID ingestionId, String action) {
    return new CompensationRepository.Compensation(UUID.randomUUID(), ingestionId, action);
  }

  private static MediaIngestion ingestion() {
    return new MediaIngestion(
        UUID.randomUUID(),
        "pepe",
        42L,
        "upload",
        Phase.FAILED,
        null,
        1,
        0,
        Instant.now(),
        Instant.now(),
        Instant.now(),
        "key",
        "movie.mp4",
        1,
        "video/mp4",
        "url");
  }
}
