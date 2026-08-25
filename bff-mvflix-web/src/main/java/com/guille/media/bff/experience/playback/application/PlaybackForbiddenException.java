package com.guille.media.bff.experience.playback.application;

/** El usuario autenticado no puede reproducir esta media (movies denegó el acceso). HTTP 403. */
public class PlaybackForbiddenException extends RuntimeException {

  public PlaybackForbiddenException(long mediaId) {
    super("Playback not allowed for media " + mediaId);
  }
}
