package com.guille.media.bff.experience.playback.infrastructure.http;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.guille.media.bff.experience.playback.application.DirectSource;
import com.guille.media.bff.experience.playback.application.PlaybackSourceUnavailableException;
import com.guille.media.bff.experience.playback.application.port.ManagedContentAccess;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.format.DateTimeParseException;

/**
 * Acceso directo a objetos MANAGED via {@code POST /catalog/streaming} de
 * storage con el machine-client M2M (scope {@code storage.stream}); el bean
 * {@code playbackWebClient} ya inyecta el Bearer. La URL presigned resultante
 * viaja al navegador y MinIO sirve los bytes con Range: el BFF no los toca.
 */
@Slf4j
@Component
public class ManagedContentAccessAdapter implements ManagedContentAccess {

  private static final String CATALOG_STREAMING = "/api/v1/movie/storage/catalog/streaming";

  private final WebClient playbackWebClient;

  public ManagedContentAccessAdapter(@Qualifier("playbackWebClient") WebClient playbackWebClient) {
    this.playbackWebClient = playbackWebClient;
  }

  @Override
  public Mono<DirectSource> openDirect(Long objectId) {
    return this.playbackWebClient
        .post()
        .uri(CATALOG_STREAMING)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(new CatalogStreamRequest(String.valueOf(objectId)))
        .retrieve()
        .bodyToMono(CatalogStreamSession.class)
        .map(session -> new DirectSource(
            session.streamingUrl(), parseExpiry(objectId, session.expiresAt()), null))
        // Nunca registrar la URL firmada: solo el objectId y la causa.
        .onErrorMap(WebClientResponseException.class, error -> {
          log.warn("catalog streaming fallo para objeto {}: status {}",
              objectId, error.getStatusCode().value());
          return new PlaybackSourceUnavailableException(
              "Contenido MANAGED no disponible (" + error.getStatusCode().value() + ")",
              error);
        })
        .onErrorMap(WebClientRequestException.class, error -> {
          log.warn("catalog streaming inalcanzable para objeto {}: {}", objectId,
              error.getMessage());
          return new PlaybackSourceUnavailableException(
              "Storage no alcanzable para el contenido", error);
        });
  }

  private Instant parseExpiry(Long objectId, String expiresAt) {
    if (expiresAt == null || expiresAt.isBlank()) {
      log.error("catalog streaming sin expiresAt para objeto {}", objectId);
      throw new PlaybackSourceUnavailableException(
          "Acceso a contenido sin expiracion declarada", null);
    }
    try {
      return Instant.parse(expiresAt);
    } catch (DateTimeParseException error) {
      log.error("catalog streaming con expiresAt invalido para objeto {}: {}", objectId,
          expiresAt.getClass().getSimpleName());
      throw new PlaybackSourceUnavailableException(
          "Acceso a contenido con expiracion ilegible", error);
    }
  }

  record CatalogStreamRequest(@JsonProperty("objectId") String objectId) {}

  record CatalogStreamSession(
      @JsonProperty("uploadId") String uploadId,
      @JsonProperty("streamingUrl") String streamingUrl,
      @JsonProperty("storageKey") String storageKey,
      @JsonProperty("expiresAt") String expiresAt,
      @JsonProperty("method") String method) {}
}
