package com.gcorp.service.app.mvflix_media_ingestion.infrastructure.messaging;

import static org.mockito.ArgumentMatchers.*; import static org.mockito.Mockito.*;
import com.gcorp.service.app.mvflix_media_ingestion.application.Outbox.Message;
import org.junit.jupiter.api.Test; import org.springframework.kafka.core.KafkaTemplate; import reactor.test.StepVerifier;
import java.time.Instant; import java.util.UUID; import java.util.concurrent.CompletableFuture;

class KafkaOutboxPublisherTest {
  @Test void publishesEachEventToItsDurableTopic() {
    KafkaTemplate<String,String> kafka=mock(KafkaTemplate.class); when(kafka.send(eq("mvflix.media-ingestion.completed.v1"),anyString(),anyString())).thenReturn(CompletableFuture.completedFuture(null));
    var message=new Message(UUID.randomUUID(),"MediaIngestionCompleted",UUID.randomUUID(),"{}",Instant.now());
    StepVerifier.create(new KafkaOutboxPublisher(kafka).publish(message)).verifyComplete();
    verify(kafka).send("mvflix.media-ingestion.completed.v1",message.aggregateId().toString(),message.payload());
  }
}
