package com.guille.media.bff.experience.playback.application;

/** La media no existe (o movies decidio no revelar su existencia). HTTP 404. */
public class PlaybackMediaNotFoundException extends RuntimeException {

  public PlaybackMediaNotFoundException(long mediaId) {
    super("Media not found: " + mediaId);
  }
}
