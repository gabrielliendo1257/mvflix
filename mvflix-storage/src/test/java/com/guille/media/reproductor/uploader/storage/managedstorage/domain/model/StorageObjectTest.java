package com.guille.media.reproductor.uploader.storage.managedstorage.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.guille.media.reproductor.uploader.storage.managedstorage.domain.exception.IllegalStateTransitionException;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.exception.InvalidObjectContentError;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.exception.StorageObjectNotAvailable;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StorageObject.StorageSessionStatus;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StorageKey;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StorageMetadata;

import org.junit.jupiter.api.Test;

import java.time.Instant;

class StorageObjectTest {

  private static final String OWNER = "user-1";

  @Test
  void isAvailableOnlyWhenCompleted() {
    assertThat(object(StorageSessionStatus.COMPLETED).isAvailable()).isTrue();
    assertThat(object(StorageSessionStatus.PENDING).isAvailable()).isFalse();
    assertThat(object(StorageSessionStatus.EXPIRED).isAvailable()).isFalse();
  }

  @Test
  void ensureAvailableDoesNotThrowWhenCompleted() {
    StorageObject object = object(StorageSessionStatus.COMPLETED);
    assertThat(object).isNotNull();
    object.ensureAvailable();
  }

  @Test
  void ensureAvailableThrowsWhenNotCompleted() {
    StorageObject object = object(StorageSessionStatus.PENDING);
    assertThatThrownBy(object::ensureAvailable)
        .isInstanceOf(StorageObjectNotAvailable.class)
        .hasMessageContaining(String.valueOf(object.getStorageId()));
  }

  @Test
  void ensureOwnedByAcceptsTheOwner() {
    StorageObject object = object(StorageSessionStatus.COMPLETED);
    object.ensureOwnedBy(OWNER);
  }

  @Test
  void ensureOwnedByRejectsOtherUsers() {
    StorageObject object = object(StorageSessionStatus.COMPLETED);
    assertThatThrownBy(() -> object.ensureOwnedBy("other"))
        .isInstanceOf(StorageObjectNotAvailable.class)
        .hasMessageContaining(String.valueOf(object.getStorageId()));
  }

  @Test
  void completeTransitionsFromPendingToCompleted() {
    StorageObject object = object(StorageSessionStatus.PENDING);
    assertThat(object.complete()).isTrue();
    assertThat(object.getStorageObjectStatus()).isEqualTo(StorageSessionStatus.COMPLETED);
  }

  @Test
  void completeIsIdempotentWhenAlreadyCompleted() {
    StorageObject object = object(StorageSessionStatus.COMPLETED);
    assertThat(object.complete()).isFalse();
    assertThat(object.getStorageObjectStatus()).isEqualTo(StorageSessionStatus.COMPLETED);
  }

  @Test
  void completeRejectsExpiredObjects() {
    StorageObject object = object(StorageSessionStatus.EXPIRED);
    assertThatThrownBy(object::complete)
        .isInstanceOf(IllegalStateTransitionException.class)
        .hasMessageContaining(String.valueOf(object.getStorageId()));
  }

  @Test
  void expireTransitionsFromPendingToExpired() {
    StorageObject object = object(StorageSessionStatus.PENDING);
    assertThat(object.expire()).isTrue();
    assertThat(object.getStorageObjectStatus()).isEqualTo(StorageSessionStatus.EXPIRED);
  }

  @Test
  void expireIsIdempotentWhenAlreadyExpired() {
    StorageObject object = object(StorageSessionStatus.EXPIRED);
    assertThat(object.expire()).isFalse();
    assertThat(object.getStorageObjectStatus()).isEqualTo(StorageSessionStatus.EXPIRED);
  }

  @Test
  void expireRejectsCompletedObjects() {
    StorageObject object = object(StorageSessionStatus.COMPLETED);
    assertThatThrownBy(object::expire)
        .isInstanceOf(IllegalStateTransitionException.class)
        .hasMessageContaining(String.valueOf(object.getStorageId()));
  }

  @Test
  void markDeletedTransitionsFromCompletedToDeleted() {
    StorageObject object = object(StorageSessionStatus.COMPLETED);
    assertThat(object.markDeleted()).isTrue();
    assertThat(object.getStorageObjectStatus()).isEqualTo(StorageSessionStatus.DELETED);
  }

  @Test
  void markDeletedIsIdempotentWhenAlreadyDeleted() {
    StorageObject object = object(StorageSessionStatus.DELETED);
    assertThat(object.markDeleted()).isFalse();
    assertThat(object.getStorageObjectStatus()).isEqualTo(StorageSessionStatus.DELETED);
  }

  @Test
  void markDeletedRejectsPendingObjects() {
    StorageObject object = object(StorageSessionStatus.PENDING);
    assertThatThrownBy(object::markDeleted)
        .isInstanceOf(IllegalStateTransitionException.class)
        .hasMessageContaining(String.valueOf(object.getStorageId()));
  }

  @Test
  void markFailedTransitionsFromPendingToFailed() {
    StorageObject object = object(StorageSessionStatus.PENDING);
    assertThat(object.markFailed()).isTrue();
    assertThat(object.getStorageObjectStatus()).isEqualTo(StorageSessionStatus.FAILED);
  }

  @Test
  void markFailedIsIdempotentWhenAlreadyFailed() {
    StorageObject object = object(StorageSessionStatus.FAILED);
    assertThat(object.markFailed()).isFalse();
    assertThat(object.getStorageObjectStatus()).isEqualTo(StorageSessionStatus.FAILED);
  }

  @Test
  void markFailedRejectsCompletedObjects() {
    StorageObject object = object(StorageSessionStatus.COMPLETED);
    assertThatThrownBy(object::markFailed)
        .isInstanceOf(IllegalStateTransitionException.class)
        .hasMessageContaining(String.valueOf(object.getStorageId()));
  }

  @Test
  void ensureValidContentLengthAcceptsExpectedSize() {
    StorageObject object = object(StorageSessionStatus.COMPLETED);
    object.ensureValidContentLength(object.sizeInBytes());
  }

  @Test
  void ensureValidContentLengthRejectsSizeMismatch() {
    StorageObject object = object(StorageSessionStatus.COMPLETED);
    assertThatThrownBy(() -> object.ensureValidContentLength(object.sizeInBytes() + 1))
        .isInstanceOf(InvalidObjectContentError.class)
        .hasMessageContaining(String.valueOf(object.getStorageId()));
  }

  private StorageObject object(StorageSessionStatus status) {
    return new StorageObject(
        OWNER,
        new StorageKey("user-1/movies/movie.mp4"),
        new StorageMetadata("video/mp4", 1024L, null, Instant.now()),
        Instant.now(),
        42L,
        status);
  }
}