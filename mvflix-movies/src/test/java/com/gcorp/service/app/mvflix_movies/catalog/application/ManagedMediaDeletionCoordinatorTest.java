package com.gcorp.service.app.mvflix_movies.catalog.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

import com.gcorp.service.app.mvflix_movies.catalog.application.port.ManagedObjectDeletion;
import com.gcorp.service.app.mvflix_movies.catalog.application.port.ManagedObjectReference;
import com.gcorp.service.app.mvflix_movies.catalog.domain.media.Media;
import com.gcorp.service.app.mvflix_movies.catalog.domain.media.MediaId;
import com.gcorp.service.app.mvflix_movies.catalog.domain.media.MediaRepository;
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
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
class ManagedMediaDeletionCoordinatorTest {

    private static final MovieId MOVIE_ID = MovieId.of(7L);

    @Mock private MediaRepository mediaRepository;
    @Mock private ManagedObjectDeletion storageDeletion;
    @Mock private MovieDeletionTransaction deletionTransaction;
    @Mock private MovieRepository movieRepository;

    @InjectMocks private ManagedMediaDeletionCoordinator coordinator;

    @Test
    void deletesManagedObjectBeforeFinalizingCatalog() {
        Movie movie = movie();
        Media media = managedMedia();
        when(this.movieRepository.findById(MOVIE_ID)).thenReturn(Mono.just(movie));
        when(this.mediaRepository.findByMovieId(MOVIE_ID)).thenReturn(Mono.just(media));
        when(this.storageDeletion.delete(new ManagedObjectReference(
                42L, "Javier", "Javier/videos/dune.mp4"))).thenReturn(Mono.empty());
        when(this.deletionTransaction.finalizeDeletion(MOVIE_ID)).thenReturn(Mono.empty());

        StepVerifier.create(this.coordinator.process(MOVIE_ID)).verifyComplete();

        InOrder order = inOrder(this.storageDeletion, this.deletionTransaction);
        order.verify(this.storageDeletion).delete(any(ManagedObjectReference.class));
        order.verify(this.deletionTransaction).finalizeDeletion(MOVIE_ID);
    }

    @Test
    void storageFailureLeavesMoviePendingAndDoesNotFinalize() {
        RuntimeException failure = new RuntimeException("storage unavailable");
        when(this.movieRepository.findById(MOVIE_ID)).thenReturn(Mono.just(movie()));
        when(this.mediaRepository.findByMovieId(MOVIE_ID)).thenReturn(Mono.just(managedMedia()));
        when(this.storageDeletion.delete(any())).thenReturn(Mono.error(failure));

        StepVerifier.create(this.coordinator.process(MOVIE_ID))
                .consumeErrorWith(error -> assertThat(error).isSameAs(failure))
                .verify();

        verify(this.deletionTransaction, never()).finalizeDeletion(any());
    }

    @Test
    void missingManagedMediaFinalizesLocalAssociationWithoutStorageCall() {
        when(this.movieRepository.findById(MOVIE_ID)).thenReturn(Mono.just(movie()));
        when(this.mediaRepository.findByMovieId(MOVIE_ID)).thenReturn(Mono.empty());
        when(this.deletionTransaction.finalizeDeletion(MOVIE_ID)).thenReturn(Mono.empty());

        StepVerifier.create(this.coordinator.process(MOVIE_ID)).verifyComplete();

        verify(this.deletionTransaction).finalizeDeletion(MOVIE_ID);
        verify(this.storageDeletion, never()).delete(any());
    }

    @Test
    void alreadyFinalizedMovieIsNoOp() {
        when(this.movieRepository.findById(MOVIE_ID)).thenReturn(Mono.empty());

        StepVerifier.create(this.coordinator.process(MOVIE_ID)).verifyComplete();

        verify(this.mediaRepository, never()).findByMovieId(any());
        verify(this.deletionTransaction, never()).finalizeDeletion(any());
    }

    private static Movie movie() {
        return new Movie(
                MOVIE_ID, "Javier", "Dune", MovieStatus.DELETING, EnrichmentStatus.ENRICHED,
                42L, (MovieMetadata) null, MovieVisibility.PRIVATE, Set.of(), MediaKind.MOVIE);
    }

    private static Media managedMedia() {
        return new Media(
                MediaId.of(11L), MOVIE_ID, 42L, "Javier/videos/dune.mp4", Instant.now());
    }
}
