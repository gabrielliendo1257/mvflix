package com.guille.media.reproductor.uploader.storage.managedstorage.infrastructure.scheduler;

import com.guille.media.reproductor.uploader.storage.managedstorage.application.StorageOutbox;
import com.guille.media.reproductor.uploader.storage.managedstorage.application.port.DeletionInboxRepository;

import org.junit.jupiter.api.Test;

import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StorageRetentionJobTest {

  private final StorageOutbox outbox = mock(StorageOutbox.class);
  private final DeletionInboxRepository inbox = mock(DeletionInboxRepository.class);

  @Test
  void purgesPublishedOutboxAndCompletedInbox() {
    when(this.outbox.purgePublishedBefore(any(Instant.class))).thenReturn(Mono.just(4L));
    when(this.inbox.purgeCompletedBefore(any(Instant.class))).thenReturn(Mono.just(7L));

    StorageRetentionJob job = new StorageRetentionJob(
        this.outbox, this.inbox, Duration.ofDays(14), Duration.ofDays(30));

    job.purgeExpiredBatch().block();

    verify(this.outbox).purgePublishedBefore(any(Instant.class));
    verify(this.inbox).purgeCompletedBefore(any(Instant.class));
  }
}
