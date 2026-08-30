package com.gcorp.service.app.mvflix_activity.application.port;

import com.gcorp.service.app.mvflix_activity.domain.PlaybackProgressed;
import com.gcorp.service.app.mvflix_activity.application.ActivityQueryService.ActivityRecord;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface WatchActivityRepository {
  Mono<Void> upsert(PlaybackProgressed event);
  Flux<ActivityRecord> history(String owner, int limit);
  Flux<ActivityRecord> continueWatching(String owner, int limit);
  Mono<ActivityRecord> movie(String owner, long movieId);
}
