package com.guille.media.bff.experience.media.infrastructure.http;

import com.guille.media.bff.app.ports.MoviesWebClient;
import com.guille.media.bff.experience.media.application.port.ProviderActions;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

/**
 * Adapter del vínculo con proveedores hacia el puerto global legacy de
 * movies. Aísla la experiencia: cuando las acciones migren a un contrato
 * dedicado, solo este archivo cambia.
 */
@Component
@RequiredArgsConstructor
public class ProviderActionsAdapter implements ProviderActions {

  private final MoviesWebClient movies;

  @Override
  public Mono<Void> link(long mediaId, long providerId) {
    return this.movies.enrichMovie(mediaId, providerId).then();
  }

  @Override
  public Mono<Void> unlink(long mediaId) {
    return this.movies.unlinkEnrichment(mediaId).then();
  }
}
