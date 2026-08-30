package com.gcorp.service.app.mvflix_media_ingestion.infrastructure.messaging;

import com.gcorp.service.app.mvflix_media_ingestion.application.Outbox;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

@Component
@ConditionalOnProperty(name="mvflix.messaging.kafka.enabled", havingValue="true")
public class MediaIngestionOutboxPublisherJob {
  private final Outbox outbox; private final KafkaOutboxPublisher publisher;
  private final int batchSize, maxAttempts, concurrency; private final Duration retryDelay, lease;
  private final AtomicLong pending = new AtomicLong(), exhausted = new AtomicLong(), oldest = new AtomicLong();
  private final Counter published, failures;

  public MediaIngestionOutboxPublisherJob(Outbox outbox, KafkaOutboxPublisher publisher, MeterRegistry metrics,
      @Value("${mvflix.outbox.batch-size:25}") int batchSize,
      @Value("${mvflix.outbox.max-attempts:10}") int maxAttempts,
      @Value("${mvflix.outbox.concurrency:4}") int concurrency,
      @Value("${mvflix.outbox.retry-delay:PT1M}") Duration retryDelay,
      @Value("${mvflix.outbox.lease:PT2M}") Duration lease) {
    this.outbox=outbox; this.publisher=publisher; this.batchSize=batchSize; this.maxAttempts=maxAttempts;
    this.concurrency=concurrency; this.retryDelay=retryDelay; this.lease=lease;
    metrics.gauge("mvflix_media_ingestion_outbox_pending", Tags.empty(), pending);
    metrics.gauge("mvflix_media_ingestion_outbox_exhausted", Tags.empty(), exhausted);
    metrics.gauge("mvflix_media_ingestion_outbox_oldest_age_seconds", Tags.empty(), oldest);
    published=metrics.counter("mvflix_media_ingestion_outbox_published_total");
    failures=metrics.counter("mvflix_media_ingestion_outbox_publish_failures_total");
  }

  @Scheduled(fixedDelayString="${mvflix.outbox.poll-interval:PT5S}")
  public void publishPending() { publishPendingBatch().subscribe(null, e -> {}); }

  public Mono<Void> publishPendingBatch() {
    return outbox.claim(batchSize,maxAttempts,lease)
      .flatMap(m -> publisher.publish(m).then(outbox.markPublished(m.eventId())).doOnSuccess(x -> published.increment())
        .onErrorResume(e -> { failures.increment(); return outbox.markFailed(m.eventId(), e.getMessage(), retryDelay); }), concurrency)
      .then(Mono.zip(outbox.pendingCount(maxAttempts),outbox.exhaustedCount(maxAttempts),outbox.oldestPendingAgeSeconds())
        .doOnNext(x -> { pending.set(x.getT1()); exhausted.set(x.getT2()); oldest.set(x.getT3()); }).then());
  }
}
