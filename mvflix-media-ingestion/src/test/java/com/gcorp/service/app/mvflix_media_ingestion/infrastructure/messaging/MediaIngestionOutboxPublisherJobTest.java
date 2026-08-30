package com.gcorp.service.app.mvflix_media_ingestion.infrastructure.messaging;

import static org.mockito.ArgumentMatchers.*; import static org.mockito.Mockito.*;
import com.gcorp.service.app.mvflix_media_ingestion.application.Outbox; import com.gcorp.service.app.mvflix_media_ingestion.application.Outbox.Message;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry; import org.junit.jupiter.api.Test; import reactor.core.publisher.Flux; import reactor.core.publisher.Mono; import reactor.test.StepVerifier;
import java.time.*; import java.util.UUID;

class MediaIngestionOutboxPublisherJobTest {
  @Test void claimsPublishesAndMarksMessage() {
    Outbox outbox=mock(Outbox.class); KafkaOutboxPublisher publisher=mock(KafkaOutboxPublisher.class); var message=new Message(UUID.randomUUID(),"MediaIngestionStarted",UUID.randomUUID(),"{}",Instant.now());
    when(outbox.claim(25,10,Duration.ofMinutes(2))).thenReturn(Flux.just(message)); when(publisher.publish(message)).thenReturn(Mono.empty()); when(outbox.markPublished(message.eventId())).thenReturn(Mono.empty());
    when(outbox.pendingCount(10)).thenReturn(Mono.just(0L)); when(outbox.exhaustedCount(10)).thenReturn(Mono.just(0L)); when(outbox.oldestPendingAgeSeconds()).thenReturn(Mono.just(0L));
    var job=new MediaIngestionOutboxPublisherJob(outbox,publisher,new SimpleMeterRegistry(),25,10,1,Duration.ofMinutes(1),Duration.ofMinutes(2));
    StepVerifier.create(job.publishPendingBatch()).verifyComplete(); verify(publisher).publish(message); verify(outbox).markPublished(message.eventId());
  }
}
