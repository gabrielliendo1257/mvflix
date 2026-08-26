package com.guille.media.bff.experience.media.web;

import com.guille.media.bff.experience.media.application.GetMediaDetail;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

/**
 * Experiencia Media Detail: "entender profundamente esta media y editarla".
 * La autorización vive en movies (ambas lecturas bajo el JWT del usuario).
 * Sin URL de reproducción: Play inicia POST /web/playback/{mediaId}/session.
 */
@Tag(name = "Media Detail", description = "Detalle profundo: overview, media, access, provider y capabilities")
@RestController
@RequestMapping(value = "/web/media", produces = MediaType.APPLICATION_JSON_VALUE)
public class MediaController {

  private final GetMediaDetail getMediaDetail;

  public MediaController(GetMediaDetail getMediaDetail) {
    this.getMediaDetail = getMediaDetail;
  }

  @Operation(summary = "Detalle completo de una media propia/visible")
  @GetMapping("/{mediaId}")
  public Mono<MediaDetailResponse> media(@PathVariable long mediaId) {
    return this.getMediaDetail.execute(mediaId).map(MediaDetailResponse::from);
  }
}
