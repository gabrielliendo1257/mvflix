package com.gcorp.service.app.mvflix_activity.application;

import com.gcorp.service.app.mvflix_activity.application.port.ActivityInbox;
import com.gcorp.service.app.mvflix_activity.application.port.WatchActivityRepository;
import com.gcorp.service.app.mvflix_activity.domain.PlaybackProgressed;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

public class ActivityProcessor {
  private final ActivityInbox inbox; private final WatchActivityRepository projection; private final TransactionalOperator tx;
  public ActivityProcessor(ActivityInbox inbox, WatchActivityRepository projection, TransactionalOperator tx) { this.inbox=inbox; this.projection=projection; this.tx=tx; }
  public Mono<Void> process(PlaybackProgressed event) {
    return inbox.recordReceived(event.eventId(), event.eventType())
        .then(tx.transactional(inbox.isCompleted(event.eventId()).flatMap(done -> done ? Mono.empty() : projection.upsert(event).then(inbox.markCompleted(event.eventId())))))
        .onErrorResume(error -> inbox.markFailed(event.eventId(), event.eventType(), error.toString()).then(Mono.error(error)));
  }
}
