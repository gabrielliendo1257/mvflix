package com.guille.media.reproductor.uploader.storage.managedstorage.domain.exception;

/** Marca una sesión de subida cancelada explícitamente por el usuario. */
public class UploadCancelledByUserException extends RuntimeException {

  public UploadCancelledByUserException() {
    super("Upload cancelled by user");
  }
}
