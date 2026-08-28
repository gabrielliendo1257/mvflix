package com.guille.media.reproductor.uploader.storage.managedstorage.infrastructure.scheduler;

import com.guille.media.reproductor.uploader.storage.managedstorage.application.StorageOutbox;
import com.guille.media.reproductor.uploader.storage.managedstorage.application.port.DeletionInboxRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
public class StorageRetentionJob {
  private final StorageOutbox outbox;
  private final DeletionInboxRepository inbox;
  private final Duration outboxRetention;
  private final Duration inboxRetention;

  public StorageRetentionJob(
      StorageOutbox outbox,
      DeletionInboxRepository inbox,
      @Value("${storage.retention.outbox-published:P14D}") Duration outboxRetention,
      @Value("${storage.retention.inbox-completed:P30D}") Duration inboxRetention) {
    this.outbox = outbox;
    this.inbox = inbox;
    this.outboxRetention = outboxRetention;
    this.inboxRetention = inboxRetention;
  }

  @Scheduled(fixedDelayString = "${storage.retention.poll-interval:PT1H}")
  public void purgeExpired() {
    purgeExpiredBatch().subscribe(
        ignored -> {}, error -> log.error("Storage retention cleanup failed", error));
  }

  public Mono<Void> purgeExpiredBatch() {
    Instant now = Instant.now();
    return Mono.zip(
            this.outbox.purgePublishedBefore(now.minus(this.outboxRetention)),
            this.inbox.purgeCompletedBefore(now.minus(this.inboxRetention)))
        .doOnNext(counts -> log.info(
            "Storage retention cleanup completed: outbox={}, inbox={}",
            counts.getT1(), counts.getT2()))
        .then();
  }
}
