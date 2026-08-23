package com.guille.media.bff.experience.addmedia.model;

/** El proceso cambió concurrentemente entre lectura y guardado. */
public class ConcurrentProcessUpdateException extends RuntimeException {

  public ConcurrentProcessUpdateException(AddMediaId id) {
    super("Actualización concurrente del proceso Add Media: " + id);
  }
}
