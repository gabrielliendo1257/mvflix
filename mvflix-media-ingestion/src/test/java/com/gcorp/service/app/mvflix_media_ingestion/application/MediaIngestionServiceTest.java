package com.gcorp.service.app.mvflix_media_ingestion.application;
import static org.assertj.core.api.Assertions.assertThat; import static org.mockito.ArgumentMatchers.*; import static org.mockito.Mockito.*; import static org.junit.jupiter.api.Assertions.*;
import com.gcorp.service.app.mvflix_media_ingestion.domain.MediaIngestion; import java.time.Instant; import java.util.UUID; import org.junit.jupiter.api.Test; import reactor.core.publisher.Mono;
class MediaIngestionServiceTest {
  @Test void completeRequestsStorageAndKeepsIngestionAwaitingUpload() {
    var fixture = fixture(MediaIngestion.Phase.AWAITING_UPLOAD);
    when(fixture.repo.compareAndSet(eq(fixture.ingestion),any())).thenReturn(Mono.just(true));
    when(fixture.clients.requestUploadCompletion("upload","a",fixture.ingestion.ingestionId()+":complete-upload"))
        .thenReturn(Mono.empty());
    when(fixture.repo.find(fixture.ingestion.ingestionId())).thenReturn(Mono.just(fixture.ingestion));

    assertThat(fixture.service.complete(fixture.ingestion.ingestionId(),"a",9L).block().phase())
        .isEqualTo(MediaIngestion.Phase.AWAITING_UPLOAD);
    verify(fixture.clients).requestUploadCompletion("upload","a",
        fixture.ingestion.ingestionId()+":complete-upload");
    verifyNoInteractions(fixture.outbox);
  }

  @Test void completeIsIdempotentWhenAlreadyCompleted() {
    var fixture = fixture(MediaIngestion.Phase.COMPLETED);
    when(fixture.repo.find(fixture.ingestion.ingestionId())).thenReturn(Mono.just(fixture.ingestion));

    assertThat(fixture.service.complete(fixture.ingestion.ingestionId(),"a",null).block())
        .isEqualTo(fixture.ingestion);
    verifyNoInteractions(fixture.clients);
  }

  @Test void completeDoesNotCallStorageWhenCasLosesRace() {
    var fixture = fixture(MediaIngestion.Phase.AWAITING_UPLOAD);
    when(fixture.repo.compareAndSet(eq(fixture.ingestion),any())).thenReturn(Mono.just(false));

    assertThrows(RuntimeException.class,
        () -> fixture.service.complete(fixture.ingestion.ingestionId(),"a",null).block());
    verifyNoInteractions(fixture.clients);
    verifyNoInteractions(fixture.outbox);
  }

  private static Fixture fixture(MediaIngestion.Phase phase) {
    var repo=mock(MediaIngestionRepository.class); var clients=mock(DownstreamClients.class);
    var outbox=mock(Outbox.class); var compensation=mock(CompensationRepository.class);
    var ingestion=new MediaIngestion(UUID.randomUUID(),"a",3L,"upload",phase,null,2,0,
        Instant.now(),Instant.now(),Instant.now(),"k","x",1,"m",null);
    return new Fixture(repo,clients,outbox, new MediaIngestionService(repo,clients,outbox,compensation),ingestion);
  }

  private record Fixture(MediaIngestionRepository repo, DownstreamClients clients, Outbox outbox,
      MediaIngestionService service, MediaIngestion ingestion) {}
}
