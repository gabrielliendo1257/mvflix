package com.guille.media.reproductor.uploader.storage.domain.exceptions;

public class IllegalStateTransitionException extends StorageException {

  public IllegalStateTransitionException(String message) {
    super(message);
  }
}