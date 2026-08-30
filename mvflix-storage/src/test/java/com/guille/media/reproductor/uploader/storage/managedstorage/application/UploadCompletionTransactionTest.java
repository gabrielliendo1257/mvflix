package com.guille.media.reproductor.uploader.storage.managedstorage.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StorageKey;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StorageMetadata;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StorageObject;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StorageObject.StorageSessionStatus;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.port.StorageRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

class UploadCompletionTransactionTest {

  private final StorageRepository repository = mock(StorageRepository.class);
  private final StorageOutbox outbox = mock(StorageOutbox.class);
  private final TransactionalOperator transactions = mock(TransactionalOperator.class);
  private final UploadCompletionTransaction transaction =
      new UploadCompletionTransaction(repository, outbox, transactions);

  @Test
  void usesIngestionIdFromIdempotencyKeyAsCorrelationId() {
    UUID ingestionId = UUID.randomUUID();
    StorageObject object = object(ingestionId + ":prepare-upload");
    when(transactions.transactional(any(Mono.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(repository.updateStatus(object, StorageSessionStatus.PENDING))
        .thenReturn(Mono.just(object));
    when(outbox.append(any())).thenReturn(Mono.empty());

    transaction.complete(object).block();

    ArgumentCaptor<StorageIntegrationEvent<?>> events =
        ArgumentCaptor.forClass(StorageIntegrationEvent.class);
    verify(outbox).append(events.capture());
    assertThat(events.getValue().correlationId()).isEqualTo(ingestionId);
  }

  @Test
  void usesEventIdForLegacyObjectWithoutIngestionIdempotencyKey() {
    StorageObject object = object(null);
    when(transactions.transactional(any(Mono.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(repository.updateStatus(object, StorageSessionStatus.PENDING))
        .thenReturn(Mono.just(object));
    when(outbox.append(any())).thenReturn(Mono.empty());

    transaction.complete(object).block();

    ArgumentCaptor<StorageIntegrationEvent<?>> events =
        ArgumentCaptor.forClass(StorageIntegrationEvent.class);
    verify(outbox).append(events.capture());
    assertThat(events.getValue().correlationId()).isEqualTo(events.getValue().eventId());
  }

  private StorageObject object(String idempotencyKey) {
    return new StorageObject(
        "pepe",
        idempotencyKey,
        new StorageKey("uploads/test.mp4"),
        new StorageMetadata("video/mp4", 1024, null, Instant.now()),
        Instant.now(),
        42L,
        StorageSessionStatus.PENDING);
  }
}
