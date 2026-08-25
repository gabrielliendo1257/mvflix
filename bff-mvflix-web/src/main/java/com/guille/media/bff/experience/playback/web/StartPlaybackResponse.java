package com.guille.media.bff.experience.playback.web;

import com.guille.media.bff.experience.playback.application.PlaybackSession;

/**
 * Contrato Angular de "quiero reproducir este contenido ahora". El front solo
 * conoce lenguaje de experiencia (media, playback, resume): nunca bucket,
 * objectKey, biblioteca ni ruta de disco.
 */
public record StartPlaybackResponse(
    String sessionId,
    MediaInfo media,
    PlaybackInfo playback,
    ResumeInfo resume) {

  public static StartPlaybackResponse from(PlaybackSession session) {
    return new StartPlaybackResponse(
        session.sessionId().toString(),
        new MediaInfo(
            session.mediaId(), session.title(), session.posterPath(), session.duration()),
        new PlaybackInfo(
            session.strategy().name(),
            session.source().url(),
            session.source().mimeType(),
            session.source().expiresAt().toString()),
        session.resumePosition() == null ? null : new ResumeInfo(
            session.resumePosition().toSeconds()));
  }

  public record MediaInfo(long id, String title, String posterPath, String duration) {}

  public record PlaybackInfo(String strategy, String url, String mimeType, String expiresAt) {}

  public record ResumeInfo(long positionSeconds) {}
}
