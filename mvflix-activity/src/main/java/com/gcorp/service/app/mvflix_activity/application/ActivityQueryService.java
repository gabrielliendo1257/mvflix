package com.gcorp.service.app.mvflix_activity.application;

import com.gcorp.service.app.mvflix_activity.application.port.WatchActivityRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class ActivityQueryService {
  public record ActivityRecord(long movieId, Long mediaId, long positionSeconds, Long durationSeconds,
      boolean completed, long sequence, java.time.Instant lastWatchedAt) {}
  private final WatchActivityRepository repository;
  public ActivityQueryService(WatchActivityRepository repository) { this.repository = repository; }
  public Flux<ActivityRecord> history(String owner, int limit) { return repository.history(owner, limit); }
  public Flux<ActivityRecord> continueWatching(String owner, int limit) { return repository.continueWatching(owner, limit); }
  public Mono<ActivityRecord> movie(String owner, long movieId) { return repository.movie(owner, movieId); }
}
