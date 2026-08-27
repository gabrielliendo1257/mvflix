package com.guille.media.bff.experience.media.infrastructure.http;

import com.guille.media.bff.app.ports.MoviesWebClient;
import com.guille.media.bff.experience.media.application.DeletionOutcome;
import com.guille.media.bff.experience.media.application.port.MediaDeletion;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Adapter del lifecycle de borrado. La semántica 204/202 la decide Movies y
 * se traduce a un outcome del contexto Media.
 */
@Component
@RequiredArgsConstructor
public class MediaDeletionAdapter implements MediaDeletion {

  private final MoviesWebClient movies;

  @Override
  public Mono<DeletionOutcome> requestDeletion(long mediaId) {
    return this.movies.requestDeletion(mediaId);
  }
}
