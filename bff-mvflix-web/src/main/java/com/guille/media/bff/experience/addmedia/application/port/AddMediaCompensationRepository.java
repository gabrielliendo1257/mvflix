package com.guille.media.bff.experience.addmedia.application.port;

import com.guille.media.bff.experience.addmedia.model.AddMediaId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AddMediaCompensationRepository {
  enum Kind { DISCARD_DRAFT, CANCEL_UPLOAD }

  record Task(long id, AddMediaId processId, Kind kind, Long resourceId, int attempts,
      String lastError) {}

  Mono<Void> enqueue(AddMediaId processId, Kind kind, Long resourceId, Throwable error);

  Flux<Task> claimPending(int limit);

  Mono<Void> markCompleted(long taskId);

  Mono<Void> markFailed(long taskId, int attempts, Throwable error);
}
