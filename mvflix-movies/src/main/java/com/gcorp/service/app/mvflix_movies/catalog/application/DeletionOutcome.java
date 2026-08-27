package com.gcorp.service.app.mvflix_movies.catalog.application;

/** Resultado estable del DELETE: terminado localmente o pendiente de reintento. */
public sealed interface DeletionOutcome
        permits DeletionOutcome.Completed, DeletionOutcome.Pending {

    record Completed() implements DeletionOutcome {}

    record Pending() implements DeletionOutcome {}
}
