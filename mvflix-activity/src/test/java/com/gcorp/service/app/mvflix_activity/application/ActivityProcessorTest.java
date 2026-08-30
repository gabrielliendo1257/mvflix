package com.gcorp.service.app.mvflix_activity.application;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;
import com.gcorp.service.app.mvflix_activity.application.port.*;
import com.gcorp.service.app.mvflix_activity.domain.PlaybackProgressed;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

class ActivityProcessorTest {
  @Test void completedEventDoesNotUpsertAgain() {
    var inbox=mock(ActivityInbox.class); var projection=mock(WatchActivityRepository.class); var tx=mock(TransactionalOperator.class);
    when(inbox.recordReceived(any(),any())).thenReturn(Mono.empty()); when(inbox.isCompleted(any())).thenReturn(Mono.just(true));
    when(tx.transactional(any(Mono.class))).thenAnswer(i -> i.getArgument(0));
    var processor=new ActivityProcessor(inbox,projection,tx); var e=new PlaybackProgressed(UUID.randomUUID().toString(),"PlaybackProgressed",1,"mvflix-playback","PlaybackSession","s","u",1,null,1,null,false,1);
    processor.process(e).block();
    verify(projection,never()).upsert(e); verify(inbox,never()).markCompleted(any());
  }
}
