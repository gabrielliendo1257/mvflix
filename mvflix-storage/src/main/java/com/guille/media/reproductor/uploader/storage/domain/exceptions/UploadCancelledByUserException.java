package com.guille.media.reproductor.uploader.storage.domain.exceptions;

/** Marca una sesión de subida cancelada explícitamente por el usuario. */
public class UploadCancelledByUserException extends RuntimeException {

  public UploadCancelledByUserException() {
    super("Upload cancelled by user");
  }
}
