package com.guille.media.bff.app.dto;

/** Detalle de película orientado a la vista: metadata + disponibilidad de reproducción. */
public record MovieDetailDto(MovieDto movie, PlaybackDto playback) {

  public record PlaybackDto(boolean available, String url) {}
}