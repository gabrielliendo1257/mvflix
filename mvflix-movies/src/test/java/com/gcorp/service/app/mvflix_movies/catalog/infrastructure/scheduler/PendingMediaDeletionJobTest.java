package com.gcorp.service.app.mvflix_movies.catalog.infrastructure.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gcorp.service.app.mvflix_movies.catalog.application.ManagedMediaDeletionCoordinator;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.EnrichmentStatus;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MediaKind;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.Movie;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieId;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieMetadata;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieRepository;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieStatus;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieVisibility;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@ExtendWith(MockitoExtension.class)
class PendingMediaDeletionJobTest {

    @Mock private MovieRepository movieRepository;
    @Mock private ManagedMediaDeletionCoordinator coordinator;

    @Test
    void findsDeletingMoviesAndRetriesEachOne() {
        Movie first = movie(1L);
        Movie second = movie(2L);
        when(this.movieRepository.findDeleting(25)).thenReturn(Flux.just(first, second));
        when(this.coordinator.process(first.getId())).thenReturn(Mono.empty());
        when(this.coordinator.process(second.getId())).thenReturn(Mono.empty());

        new PendingMediaDeletionJob(this.movieRepository, this.coordinator, 25, 4)
                .retryPendingBatch().block();

        verify(this.coordinator).process(first.getId());
        verify(this.coordinator).process(second.getId());
    }

    @Test
    void oneFailureDoesNotStopTheBatch() {
        Movie failed = movie(1L);
        Movie recovered = movie(2L);
        when(this.movieRepository.findDeleting(25)).thenReturn(Flux.just(failed, recovered));
        when(this.coordinator.process(failed.getId()))
                .thenReturn(Mono.error(new RuntimeException("Storage unavailable")));
        when(this.coordinator.process(recovered.getId())).thenReturn(Mono.empty());

        new PendingMediaDeletionJob(this.movieRepository, this.coordinator, 25, 4)
                .retryPendingBatch().block();

        verify(this.coordinator).process(failed.getId());
        verify(this.coordinator).process(recovered.getId());
    }

    @Test
    void usesConfiguredBatchSizeAndConcurrency() {
        when(this.movieRepository.findDeleting(7)).thenReturn(Flux.empty());
        new PendingMediaDeletionJob(this.movieRepository, this.coordinator, 7, 2)
                .retryPendingBatch().block();

        verify(this.movieRepository).findDeleting(7);
        verify(this.coordinator, never()).process(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void concurrentInvocationsDelegateToIdempotentCoordinator() {
        Movie pending = movie(1L);
        AtomicInteger attempts = new AtomicInteger();
        when(this.movieRepository.findDeleting(25)).thenReturn(Flux.just(pending));
        when(this.coordinator.process(pending.getId())).thenAnswer(invocation -> {
            attempts.incrementAndGet();
            return Mono.empty();
        });

        PendingMediaDeletionJob job = new PendingMediaDeletionJob(
                this.movieRepository, this.coordinator, 25, 4);
        Mono.when(job.retryPendingBatch(), job.retryPendingBatch()).block();

        assertThat(attempts).hasValue(2);
        verify(this.coordinator, org.mockito.Mockito.times(2)).process(pending.getId());
    }

    private static Movie movie(long id) {
        return new Movie(
                MovieId.of(id), "pepe", "Dune", MovieStatus.DELETING, EnrichmentStatus.ENRICHED,
                null, (MovieMetadata) null, MovieVisibility.PRIVATE, Set.of(), MediaKind.MOVIE);
    }
}
