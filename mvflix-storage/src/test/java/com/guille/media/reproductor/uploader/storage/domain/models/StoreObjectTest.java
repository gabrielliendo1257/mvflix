package com.guille.media.reproductor.uploader.storage.domain.models;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.guille.media.reproductor.uploader.storage.domain.exceptions.InvalidObjectContentError;
import com.guille.media.reproductor.uploader.storage.domain.exceptions.StorageObjectNotAvailable;
import com.guille.media.reproductor.uploader.storage.domain.models.StoreObject.StorageSessionStatus;
import com.guille.media.reproductor.uploader.storage.domain.vos.StorageKey;
import com.guille.media.reproductor.uploader.storage.domain.vos.StorageMetadata;

import org.junit.jupiter.api.Test;

import java.time.Instant;

class StoreObjectTest {

  private static final String OWNER = "user-1";

  @Test
  void isAvailableOnlyWhenCompleted() {
    assertThat(object(StorageSessionStatus.COMPLETED).isAvailable()).isTrue();
    assertThat(object(StorageSessionStatus.PENDING).isAvailable()).isFalse();
    assertThat(object(StorageSessionStatus.EXPIRED).isAvailable()).isFalse();
  }

  @Test
  void ensureAvailableDoesNotThrowWhenCompleted() {
    StoreObject object = object(StorageSessionStatus.COMPLETED);
    assertThat(object).isNotNull();
    object.ensureAvailable();
  }

  @Test
  void ensureAvailableThrowsWhenNotCompleted() {
    StoreObject object = object(StorageSessionStatus.PENDING);
    assertThatThrownBy(object::ensureAvailable)
        .isInstanceOf(StorageObjectNotAvailable.class)
        .hasMessageContaining(String.valueOf(object.getStorageId()));
  }

  @Test
  void ensureOwnedByAcceptsTheOwner() {
    StoreObject object = object(StorageSessionStatus.COMPLETED);
    object.ensureOwnedBy(OWNER);
  }

  @Test
  void ensureOwnedByRejectsOtherUsers() {
    StoreObject object = object(StorageSessionStatus.COMPLETED);
    assertThatThrownBy(() -> object.ensureOwnedBy("other"))
        .isInstanceOf(StorageObjectNotAvailable.class)
        .hasMessageContaining(String.valueOf(object.getStorageId()));
  }

  @Test
  void ensureValidContentLengthAcceptsExpectedSize() {
    StoreObject object = object(StorageSessionStatus.COMPLETED);
    object.ensureValidContentLength(object.sizeInBytes());
  }

  @Test
  void ensureValidContentLengthRejectsSizeMismatch() {
    StoreObject object = object(StorageSessionStatus.COMPLETED);
    assertThatThrownBy(() -> object.ensureValidContentLength(object.sizeInBytes() + 1))
        .isInstanceOf(InvalidObjectContentError.class)
        .hasMessageContaining(String.valueOf(object.getStorageId()));
  }

  private StoreObject object(StorageSessionStatus status) {
    return new StoreObject(
        OWNER,
        new StorageKey("user-1/movies/movie.mp4"),
        new StorageMetadata("video/mp4", 1024L, null, Instant.now()),
        Instant.now(),
        42L,
        status);
  }
}