package com.guille.media.bff.experience.addmedia.web;

import com.guille.media.bff.app.dto.CreateMovieRequest;
import com.guille.media.bff.experience.addmedia.application.StartAddMediaCommand;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import org.hibernate.validator.constraints.Length;

import java.util.List;

/**
 * Intención completa del usuario para dar de alta contenido. El front expresa
 * QUÉ quiere añadir; la coreografía (draft → upload → verify) es del BFF.
 *
 * @param idempotencyKey clave generada por el front por intento; replays con
 *        la misma clave no duplican draft/upload.
 */
public record StartAddMediaRequest(
    @Valid @NotNull FileSelection file,
    @Valid @NotNull MovieSelection movie,
    InitialAccess access,
    @NotBlank @Length(max = 128) String idempotencyKey) {

  public record FileSelection(
      @NotBlank String filename,
      @Positive long sizeBytes,
      @NotBlank String mimeType) {}

  /**
   * V1: el front envía el draft completo ya resuelto desde el preview.
   * Los overrides finos de metadata TMDB quedan diferidos al enrichment de
   * Movies, dueño del proveedor.
   */
  public record MovieSelection(Long providerId, @Valid @NotNull CreateMovieRequest draft) {}

  /**
   * Preferencia inicial de acceso. PRIVATE es el default presentado en UI,
   * pero quien APLICA y valida la política es Movies: el BFF no es el único
   * guardián de que SHARED requiera usuarios.
   */
  public record InitialAccess(String visibility, List<String> sharedWith) {}


  public StartAddMediaCommand toCommand() {
    return toCommand(this.idempotencyKey);
  }

  public StartAddMediaCommand toCommand(String effectiveIdempotencyKey) {
    return new StartAddMediaCommand(
        new StartAddMediaCommand.FileSelection(
            file.filename(), file.sizeBytes(), file.mimeType()),
        new StartAddMediaCommand.MovieSelection(
            movie.providerId(), movie.draft()),
        access == null
            ? new StartAddMediaCommand.InitialAccess(
                null, null)
            : new StartAddMediaCommand.InitialAccess(
                access.visibility(), access.sharedWith()),
        effectiveIdempotencyKey);
  }
}
