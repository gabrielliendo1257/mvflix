package com.gcorp.service.app.mvflix_media_ingestion.application;
import static org.mockito.ArgumentMatchers.*; import static org.mockito.Mockito.*; import static org.junit.jupiter.api.Assertions.*;
import com.gcorp.service.app.mvflix_media_ingestion.domain.MediaIngestion; import java.time.Instant; import java.util.UUID; import org.junit.jupiter.api.Test; import reactor.core.publisher.Mono;
class MediaIngestionServiceTest {
  @Test void completeDoesNotCallDownstreamWhenCasLosesRace(){var repo=mock(MediaIngestionRepository.class); var clients=mock(DownstreamClients.class); var outbox=mock(Outbox.class); var compensation=mock(CompensationRepository.class); var i=new MediaIngestion(UUID.randomUUID(),"a",3L,"upload",MediaIngestion.Phase.AWAITING_UPLOAD,null,2,0,Instant.now(),Instant.now(),Instant.now(),"k","x",1,"m",null); when(repo.find(i.ingestionId())).thenReturn(Mono.just(i)); when(repo.compareAndSet(eq(i),any())).thenReturn(Mono.just(false)); var service=new MediaIngestionService(repo,clients,outbox,compensation); assertThrows(RuntimeException.class,()->service.complete(i.ingestionId(),"a",9,"object").block()); verifyNoInteractions(clients); verifyNoInteractions(outbox); }
}
