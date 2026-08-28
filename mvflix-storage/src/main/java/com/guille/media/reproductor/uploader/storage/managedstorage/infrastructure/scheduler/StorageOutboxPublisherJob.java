package com.guille.media.reproductor.uploader.storage.managedstorage.infrastructure.scheduler;

import com.guille.media.reproductor.uploader.storage.managedstorage.application.StoredObjectDeletedOutbox;
import com.guille.media.reproductor.uploader.storage.managedstorage.infrastructure.events.KafkaStorageOutboxPublisher;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
@ConditionalOnProperty(name = "mvflix.messaging.kafka.enabled", havingValue = "true")
public class StorageOutboxPublisherJob {

  private final StoredObjectDeletedOutbox outbox;
  private final KafkaStorageOutboxPublisher publisher;
  private final int batchSize;
  private final int maxAttempts;
  private final int concurrency;
  private final Duration retryDelay;
  private final Duration lease;
  private final AtomicLong backlog = new AtomicLong();
  private final AtomicLong exhausted = new AtomicLong();
  private final AtomicLong oldestAgeSeconds = new AtomicLong();
  private final Counter published;
  private final Counter failed;

  public StorageOutboxPublisherJob(
      StoredObjectDeletedOutbox outbox,
      KafkaStorageOutboxPublisher publisher,
      MeterRegistry meterRegistry,
      @Value("${storage.outbox.batch-size:25}") int batchSize,
      @Value("${storage.outbox.max-attempts:10}") int maxAttempts,
      @Value("${storage.outbox.concurrency:4}") int concurrency,
      @Value("${storage.outbox.retry-delay:PT1M}") Duration retryDelay,
      @Value("${storage.outbox.lease:PT2M}") Duration lease) {
    this.outbox = outbox;
    this.publisher = publisher;
    this.batchSize = batchSize;
    this.maxAttempts = maxAttempts;
    this.concurrency = concurrency;
    this.retryDelay = retryDelay;
    this.lease = lease;
    meterRegistry.gauge("mvflix_outbox_pending", Tags.of("service", "storage"), this.backlog);
    meterRegistry.gauge("mvflix_outbox_exhausted", Tags.of("service", "storage"), this.exhausted);
    meterRegistry.gauge("mvflix_outbox_oldest_age_seconds", Tags.of("service", "storage"), this.oldestAgeSeconds);
    this.published = meterRegistry.counter("mvflix_outbox_published_total", "service", "storage");
    this.failed = meterRegistry.counter("mvflix_outbox_publish_failures_total", "service", "storage");
  }

  @Scheduled(fixedDelayString = "${storage.outbox.poll-interval:PT5S}")
  public void publishPending() {
    this.publishPendingBatch().subscribe(
        ignored -> {}, error -> log.error("Error claiming Storage outbox", error));
  }

  public Mono<Void> publishPendingBatch() {
    return this.outbox.claim(this.batchSize, this.maxAttempts, this.lease)
        .flatMap(message -> this.publisher.publish(message)
            .then(this.outbox.markPublished(message.eventId()))
            .doOnSuccess(ignored -> this.published.increment())
            .onErrorResume(error -> {
              this.failed.increment();
              return this.outbox.markFailed(message.eventId(), error.getMessage(), this.retryDelay)
                  .onErrorResume(markError -> {
                    log.error("Could not mark Storage outbox failure", markError);
                    return Mono.empty();
                  });
            }), this.concurrency)
        .then(Mono.zip(this.outbox.pendingCount(this.maxAttempts),
            this.outbox.exhaustedCount(this.maxAttempts), this.outbox.oldestPendingAgeSeconds())
            .doOnNext(counts -> {
              this.backlog.set(counts.getT1());
              this.exhausted.set(counts.getT2());
              this.oldestAgeSeconds.set(counts.getT3());
            }).then());
  }
}
