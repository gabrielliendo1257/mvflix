package com.guille.media.bff.experience.media.application;

/** Resultado del lifecycle de borrado decidido por Movies. */
public sealed interface DeletionOutcome
    permits DeletionOutcome.Completed, DeletionOutcome.Pending {

  record Completed() implements DeletionOutcome {}

  record Pending() implements DeletionOutcome {}
}
