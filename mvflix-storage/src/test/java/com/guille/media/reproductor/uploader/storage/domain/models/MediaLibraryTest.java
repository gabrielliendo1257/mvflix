package com.guille.media.reproductor.uploader.storage.domain.models;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class MediaLibraryTest {

  private MediaLibrary library(boolean enabled, String owner) {
    return new MediaLibrary(1L, MediaLibraryType.LOCAL, "/tmp/media", enabled, owner,
        Instant.now());
  }

  @Test
  void operatorLibraryIsAccessibleToAnyAuthenticatedUser() {
    assertThat(this.library(true, null).isAccessibleTo("pepe")).isTrue();
    assertThat(this.library(true, null).isAccessibleTo("ana")).isTrue();
  }

  @Test
  void ownedLibraryIsAccessibleOnlyToItsOwner() {
    assertThat(this.library(true, "pepe").isAccessibleTo("pepe")).isTrue();
    assertThat(this.library(true, "pepe").isAccessibleTo("ana")).isFalse();
  }

  @Test
  void disabledLibraryIsNotAccessibleEvenToOwnerOrAnyone() {
    assertThat(this.library(false, "pepe").isAccessibleTo("pepe")).isFalse();
    assertThat(this.library(false, null).isAccessibleTo("pepe")).isFalse();
  }
}
