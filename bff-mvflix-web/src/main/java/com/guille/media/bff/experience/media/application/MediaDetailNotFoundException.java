package com.guille.media.bff.experience.media.application;

/** La media no existe o movies decidió no revelar su existencia. HTTP 404. */
public class MediaDetailNotFoundException extends RuntimeException {

  public MediaDetailNotFoundException(long mediaId) {
    super("Media not found: " + mediaId);
  }
}
