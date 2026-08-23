package com.guille.media.bff.experience.addmedia.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AddMediaProcessTest {

  private final AddMediaId id = AddMediaId.newId();

  @Test
  void startingToWaitingForUploadCarriesCorrelation() {
    AddMediaProcess process = AddMediaProcess.starting(this.id, "pepe");

    AddMediaProcess prepared = process.uploadPrepared(11L, 42L);

    assertThat(prepared.phase()).isEqualTo(AddMediaPhase.WAITING_FOR_UPLOAD);
    assertThat(prepared.movieId()).isEqualTo(11L);
    assertThat(prepared.uploadId()).isEqualTo(42L);
    assertThat(prepared.version()).isEqualTo(process.version() + 1);
    assertThat(prepared.ownedBy("pepe")).isTrue();
    assertThat(prepared.ownedBy("ana")).isFalse();
  }

  @Test
  void happyPathTransitionsAreVersionedAndImmutable() {
    AddMediaProcess process = AddMediaProcess.starting(this.id, "pepe")
        .uploadPrepared(11L, 42L)
        .verifying()
        .ready();

    assertThat(process.phase()).isEqualTo(AddMediaPhase.READY);
    assertThat(process.failureCode()).isNull();
  }

  @Test
  void verifyingIsIdempotentWhileAlreadyVerifying() {
    AddMediaProcess verifying = AddMediaProcess.starting(this.id, "pepe")
        .uploadPrepared(11L, 42L)
        .verifying();

    AddMediaProcess again = verifying.verifying();

    assertThat(again).isSameAs(verifying);
    assertThat(again.version()).isEqualTo(verifying.version());
  }

  @Test
  void invalidJumpsAreRejected() {
    AddMediaProcess starting = AddMediaProcess.starting(this.id, "pepe");
    assertThatThrownBy(starting::ready).isInstanceOf(InvalidAddMediaTransition.class);
    assertThatThrownBy(starting::verifying).isInstanceOf(InvalidAddMediaTransition.class);

    AddMediaProcess ready = starting.uploadPrepared(1L, 2L).verifying().ready();
    assertThatThrownBy(() -> ready.failed("X")).isInstanceOf(InvalidAddMediaTransition.class);
    assertThatThrownBy(ready::cancelled).isInstanceOf(InvalidAddMediaTransition.class);
  }

  @Test
  void failureCarriesScreenOrientedCode() {
    AddMediaProcess failed =
        AddMediaProcess.starting(this.id, "pepe").uploadPrepared(1L, 2L).failed("UPLOAD_FAILED");

    assertThat(failed.phase()).isEqualTo(AddMediaPhase.FAILED);
    assertThat(failed.failureCode()).isEqualTo("UPLOAD_FAILED");
  }

  @Test
  void cancelIsAllowedWhileWaitingForUpload() {
    AddMediaProcess cancelled =
        AddMediaProcess.starting(this.id, "pepe").uploadPrepared(1L, 2L).cancelled();

    assertThat(cancelled.phase()).isEqualTo(AddMediaPhase.CANCELLED);
  }
}
