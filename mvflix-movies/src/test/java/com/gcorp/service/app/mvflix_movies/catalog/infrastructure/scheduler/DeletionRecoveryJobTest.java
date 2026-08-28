package com.gcorp.service.app.mvflix_movies.catalog.infrastructure.scheduler;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gcorp.service.app.mvflix_movies.catalog.application.MovieDeletionTransaction;
import com.gcorp.service.app.mvflix_movies.catalog.application.port.ManagedDeletionOutbox;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.EnrichmentStatus;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MediaKind;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.Movie;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieId;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieMetadata;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieRepository;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieStatus;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieVisibility;

import org.junit.jupiter.api.Test;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;

class DeletionRecoveryJobTest {

    private final MovieRepository movieRepository = org.mockito.Mockito.mock(MovieRepository.class);
    private final MovieDeletionTransaction transaction = org.mockito.Mockito.mock(MovieDeletionTransaction.class);
    private final ManagedDeletionOutbox outbox = org.mockito.Mockito.mock(ManagedDeletionOutbox.class);

    @Test
    void ensuresDeletingMoviesAndReactivatesExhaustedOutbox() {
        Movie movie = new Movie(MovieId.of(7L), "pepe", "Dune", MovieStatus.DELETING,
                EnrichmentStatus.ENRICHED, null, (MovieMetadata) null, MovieVisibility.PRIVATE,
                Set.of(), MediaKind.MOVIE);
        when(movieRepository.findDeleting(25)).thenReturn(Flux.just(movie));
        when(transaction.ensureDeletionRequested(movie.getId())).thenReturn(Mono.empty());
        when(outbox.reactivateExhausted("7", 10)).thenReturn(Mono.empty());

        new DeletionRecoveryJob(movieRepository, transaction, outbox, 25, 10).recoverBatch().block();

        verify(transaction).ensureDeletionRequested(movie.getId());
        verify(outbox).reactivateExhausted("7", 10);
    }
}
