package com.guille.media.bff.experience.playback.infrastructure.http;

import static org.assertj.core.api.Assertions.assertThat;

import com.guille.media.bff.experience.playback.application.LocalStreamTokenException;
import com.guille.media.bff.experience.playback.application.port.LocalPlaybackAccess;

import org.junit.jupiter.api.Test;

import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.Instant;

class HmacLocalPlaybackAccessTest {

  private final HmacLocalPlaybackAccess access =
      new HmacLocalPlaybackAccess("unit-secret", Duration.ofHours(2));

  private LocalPlaybackAccess.LocalMintCommand command() {
    return new LocalPlaybackAccess.LocalMintCommand(
        42L, 5L, 3L, "Movies/edward scissorhands.mkv", "pepe");
  }

  @Test
  void mintedTokenResolvesToSameBinding() {
    LocalPlaybackAccess.MintedAccess minted = this.access.mint(command())
        .block(Duration.ofSeconds(1));
    Instant before = Instant.now();

    StepVerifier.create(this.access.resolve(minted.rawToken()))
        .assertNext(grant -> {
          assertThat(grant.mediaId()).isEqualTo(42L);
          assertThat(grant.assetId()).isEqualTo(5L);
          assertThat(grant.libraryId()).isEqualTo(3L);
          assertThat(grant.relativePath()).isEqualTo("Movies/edward scissorhands.mkv");
          assertThat(grant.subject()).isEqualTo("pepe");
          assertThat(grant.expiresAt()).isAfter(before);
        })
        .verifyComplete();
  }

  @Test
  void tamperedTokenIsRejected() {
    LocalPlaybackAccess.MintedAccess minted = this.access.mint(command())
        .block(Duration.ofSeconds(1));
    String tampered = minted.rawToken().substring(0, minted.rawToken().length() - 4) + "AAAA";

    StepVerifier.create(this.access.resolve(tampered))
        .expectError(LocalStreamTokenException.class)
        .verify();
  }

  @Test
  void tokenSignedWithOtherSecretIsRejected() {
    var otherKeyAccess = new HmacLocalPlaybackAccess("other-secret", Duration.ofHours(2));
    LocalPlaybackAccess.MintedAccess foreign = otherKeyAccess.mint(command())
        .block(Duration.ofSeconds(1));

    StepVerifier.create(this.access.resolve(foreign.rawToken()))
        .expectError(LocalStreamTokenException.class)
        .verify();
  }

  @Test
  void expiredCapabilityIsRejected() {
    var shortTtl = new HmacLocalPlaybackAccess("unit-secret", Duration.ofSeconds(-5));
    LocalPlaybackAccess.MintedAccess expired = shortTtl.mint(command())
        .block(Duration.ofSeconds(1));

    StepVerifier.create(this.access.resolve(expired.rawToken()))
        .expectError(LocalStreamTokenException.class)
        .verify();
  }

  @Test
  void blankOrMissingCapabilityIsRejectedWithoutParsing() {
    StepVerifier.create(this.access.resolve(null))
        .expectError(LocalStreamTokenException.class)
        .verify();
    StepVerifier.create(this.access.resolve("   "))
        .expectError(LocalStreamTokenException.class)
        .verify();
  }
}
