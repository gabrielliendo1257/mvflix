package com.guille.media.bff.experience.media.web;

import com.guille.media.bff.experience.media.application.ChangeMediaAccess;
import com.guille.media.bff.experience.media.application.EditMediaMetadata;
import com.guille.media.bff.experience.media.application.GetMediaDetail;
import com.guille.media.bff.experience.media.application.LinkMediaProvider;
import com.guille.media.bff.experience.media.application.MetadataPatch;
import com.guille.media.bff.experience.media.application.UnlinkMediaProvider;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
  private final LinkMediaProvider linkMediaProvider;
  private final UnlinkMediaProvider unlinkMediaProvider;
  private final EditMediaMetadata editMediaMetadata;
  private final ChangeMediaAccess changeMediaAccess;

  public MediaController(
      GetMediaDetail getMediaDetail,
      LinkMediaProvider linkMediaProvider,
      UnlinkMediaProvider unlinkMediaProvider,
      EditMediaMetadata editMediaMetadata,
      ChangeMediaAccess changeMediaAccess) {
    this.getMediaDetail = getMediaDetail;
    this.linkMediaProvider = linkMediaProvider;
    this.unlinkMediaProvider = unlinkMediaProvider;
    this.editMediaMetadata = editMediaMetadata;
    this.changeMediaAccess = changeMediaAccess;
  }

  @Operation(summary = "Detalle completo de una media propia/visible")
  @GetMapping("/{mediaId}")
  public Mono<MediaDetailResponse> media(@PathVariable long mediaId) {
    return this.getMediaDetail.execute(mediaId).map(MediaDetailResponse::from);
  }

  /** Vincula la media a un candidato del proveedor; responde con el detalle actualizado. */
  @Operation(summary = "Vincula la media al proveedor (tmdbId); devuelve el detalle refrescado")
  @PostMapping(value = "/{mediaId}/provider", consumes = MediaType.APPLICATION_JSON_VALUE)
  public Mono<MediaDetailResponse> linkProvider(
      @PathVariable long mediaId, @RequestBody LinkProviderRequest request) {
    return this.linkMediaProvider
        .execute(mediaId, request.tmdbId())
        .map(MediaDetailResponse::from);
  }

  /** Desvincula la media del proveedor; responde con el detalle actualizado. */
  @Operation(summary = "Desvincula la media del proveedor; devuelve el detalle refrescado")
  @DeleteMapping("/{mediaId}/provider")
  public Mono<MediaDetailResponse> unlinkProvider(@PathVariable long mediaId) {
    return this.unlinkMediaProvider
        .execute(mediaId)
        .map(MediaDetailResponse::from);
  }

  /** Edita metadata (merge: null conserva); responde con el detalle actualizado. */
  @Operation(summary = "Edita la metadata de la media; devuelve el detalle refrescado")
  @PutMapping(value = "/{mediaId}/metadata", consumes = MediaType.APPLICATION_JSON_VALUE)
  public Mono<MediaDetailResponse> editMetadata(
      @PathVariable long mediaId, @RequestBody MetadataRequest request) {
    return this.editMediaMetadata
        .execute(mediaId, request.toPatch())
        .map(MediaDetailResponse::from);
  }

  /** Contrato camelCase limpio para el front; el merge lo aplica movies. */
  public record MetadataRequest(
      String title,
      String originalTitle,
      Integer year,
      List<String> genres,
      String duration,
      String director,
      List<String> cast,
      String overview,
      String posterUrl,
      String releaseDate,
      String country,
      String language,
      List<String> awards,
      Double popularity,
      String kind) {

    MetadataPatch toPatch() {
      return new MetadataPatch(
          title(), originalTitle(), year(), genres(), duration(),
          director(), cast(), overview(), posterUrl(), releaseDate(),
          country(), language(), awards(), popularity(), kind());
    }
  }

  /**
   * Un solo contrato para changeVisibility y manageSharing: ambos se aplican
   * atómicamente en movies y la respuesta trae el detalle refrescado.
   */
  @Operation(summary = "Cambia el acceso completo (visibilidad + compartidos); detalle refrescado")
  @PutMapping(value = "/{mediaId}/access", consumes = MediaType.APPLICATION_JSON_VALUE)
  public Mono<MediaDetailResponse> changeAccess(
      @PathVariable long mediaId, @RequestBody ChangeAccessRequest request) {
    return this.changeMediaAccess
        .execute(mediaId, request.visibility(), request.sharedWith())
        .map(MediaDetailResponse::from);
  }

  public record ChangeAccessRequest(String visibility, java.util.List<String> sharedWith) {}

  public record LinkProviderRequest(Long tmdbId) {}
}
