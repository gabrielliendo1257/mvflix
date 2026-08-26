package com.guille.media.bff.experience.media.infrastructure.http;

import com.guille.media.bff.app.ports.MoviesWebClient;
import com.guille.media.bff.experience.media.application.port.AccessActions;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

import java.util.List;

/** Adapter hacia el endpoint atómico de acceso en movies. */
@Component
@RequiredArgsConstructor
public class AccessActionsAdapter implements AccessActions {

  private final MoviesWebClient movies;

  @Override
  public Mono<Void> updateAccess(long mediaId, String visibility, List<String> sharedWith) {
    return this.movies.updateMovieAccess(mediaId, visibility, sharedWith).then();
  }
}
