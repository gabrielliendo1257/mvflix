package com.guille.media.reproductor.uploader.storage.managedstorage.domain.exception;

/** El objeto se removió del object store antes de completar la sesión de subida. */
public class StorageObjectRemovedException extends RuntimeException {

  public StorageObjectRemovedException() {
    super("Storage object removed before upload completed");
  }
}
