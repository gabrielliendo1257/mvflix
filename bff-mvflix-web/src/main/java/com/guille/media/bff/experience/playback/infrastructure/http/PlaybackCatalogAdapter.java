package com.guille.media.bff.experience.playback.infrastructure.http;

import com.guille.media.bff.app.dto.MediaAssetDto;
import com.guille.media.bff.app.dto.MovieDto;
import com.guille.media.bff.experience.playback.application.AssetNotPlayableException;
import com.guille.media.bff.experience.playback.application.PlayableAsset;
import com.guille.media.bff.experience.playback.application.PlaybackForbiddenException;
import com.guille.media.bff.experience.playback.application.PlaybackMediaNotFoundException;
import com.guille.media.bff.experience.playback.application.PlaybackSourceUnavailableException;
import com.guille.media.bff.experience.playback.application.port.PlaybackCatalog;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Mono;

/**
 * Adapter del puerto {@link PlaybackCatalog} contra mvflix-movies. La
 * autorización REAL ocurre en movies ({@code isVisibleTo} bajo el JWT del
 * usuario, tanto al servir la media como el asset): aquí solo se traducen sus
 * respuestas a errores de la experiencia. Las dos lecturas corren en paralelo.
 */
@Component
public class PlaybackCatalogAdapter implements PlaybackCatalog {

  private static final String API = "/api/v1/movies";

  private static final MediaAssetDto NO_ASSET = new MediaAssetDto(null, null, null, 0, null, null, null);

  private final WebClient moviesWebClient;

  public PlaybackCatalogAdapter(@Qualifier("moviesWebClient") WebClient moviesWebClient) {
    this.moviesWebClient = moviesWebClient;
  }

  @Override
  public Mono<PlaybackMedia> loadVisibleMedia(long mediaId) {
    var movie = this.moviesWebClient
        .get()
        .uri(API + "/" + mediaId)
        .retrieve()
        .bodyToMono(MovieDto.class)
        // movies responde 403 cuando la media no existe o no es visible (no
        // revela existencia); un 404 real queda como defensa.
        .onErrorMap(WebClientResponseException.class, error -> translateMovie(mediaId, error));
    var asset = this.moviesWebClient
        .get()
        .uri(API + "/media-assets/by-movie/" + mediaId)
        .retrieve()
        .bodyToMono(MediaAssetDto.class)
        // Sin asset vinculado (media recién creada) es estado del contenido,
        // no fallo de permisos ni de infraestructura.
        .onErrorResume(WebClientResponseException.NotFound.class, error -> Mono.empty())
        .onErrorMap(WebClientResponseException.class, error -> translate(mediaId, error));
    return Mono.zip(movie, asset.defaultIfEmpty(NO_ASSET))
        .map(joined -> new PlaybackMedia(
            new PlaybackMovie(
                joined.getT1().id(), joined.getT1().title(), joined.getT1().status(),
                joined.getT1().posterPath(), joined.getT1().duration(),
                joined.getT1().objectId()),
            joined.getT2().id() == null ? null : new PlayableAsset(
                joined.getT2().id(), mediaId, joined.getT2().mimeType(),
                joined.getT2().size(),
                joined.getT1().objectId(),
                joined.getT2().libraryId(), joined.getT2().relativePath())));
  }

  private RuntimeException translateMovie(long mediaId, WebClientResponseException error) {
    int status = error.getStatusCode().value();
    if (status == 404) {
      return new PlaybackMediaNotFoundException(mediaId);
    }
    if (status == 403 || status == 401) {
      return new PlaybackForbiddenException(mediaId);
    }
    return unavailable(error);
  }

  private RuntimeException translate(long mediaId, WebClientResponseException error) {
    int status = error.getStatusCode().value();
    if (status == 403 || status == 401) {
      return new PlaybackForbiddenException(mediaId);
    }
    return unavailable(error);
  }

  static RuntimeException unavailable(WebClientResponseException error) {
    return new PlaybackSourceUnavailableException(
        "Catalogo no disponible (" + error.getStatusCode().value() + ")", error);
  }
}
