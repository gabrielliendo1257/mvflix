package com.gcorp.service.app.mvflix_movies.catalog.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

import com.gcorp.service.app.mvflix_movies.catalog.application.port.ManagedObjectDeletion;
import com.gcorp.service.app.mvflix_movies.catalog.application.port.ManagedObjectReference;
import com.gcorp.service.app.mvflix_movies.catalog.domain.media.ManagedMediaAsset;
import com.gcorp.service.app.mvflix_movies.catalog.domain.media.MediaId;
import com.gcorp.service.app.mvflix_movies.catalog.domain.media.MediaRepository;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.EnrichmentStatus;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MediaKind;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItem;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItemId;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieMetadata;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItemRepository;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItemStatus;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItemVisibility;

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

    private static final CatalogItemId MOVIE_ID = CatalogItemId.of(7L);

    @Mock private MediaRepository mediaRepository;
    @Mock private ManagedObjectDeletion storageDeletion;
    @Mock private CatalogItemDeletionTransaction deletionTransaction;
    @Mock private CatalogItemRepository movieRepository;

    @InjectMocks private ManagedMediaDeletionCoordinator coordinator;

    @Test
    void deletesManagedObjectBeforeFinalizingCatalog() {
        CatalogItem movie = movie();
        ManagedMediaAsset media = managedMedia();
        when(this.movieRepository.findById(MOVIE_ID)).thenReturn(Mono.just(movie));
        when(this.mediaRepository.findByCatalogItemId(MOVIE_ID)).thenReturn(Mono.just(media));
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
        when(this.mediaRepository.findByCatalogItemId(MOVIE_ID)).thenReturn(Mono.just(managedMedia()));
        when(this.storageDeletion.delete(any())).thenReturn(Mono.error(failure));

        StepVerifier.create(this.coordinator.process(MOVIE_ID))
                .consumeErrorWith(error -> assertThat(error).isSameAs(failure))
                .verify();

        verify(this.deletionTransaction, never()).finalizeDeletion(any());
    }

    @Test
    void missingManagedMediaFinalizesLocalAssociationWithoutStorageCall() {
        when(this.movieRepository.findById(MOVIE_ID)).thenReturn(Mono.just(movie()));
        when(this.mediaRepository.findByCatalogItemId(MOVIE_ID)).thenReturn(Mono.empty());
        when(this.deletionTransaction.finalizeDeletion(MOVIE_ID)).thenReturn(Mono.empty());

        StepVerifier.create(this.coordinator.process(MOVIE_ID)).verifyComplete();

        verify(this.deletionTransaction).finalizeDeletion(MOVIE_ID);
        verify(this.storageDeletion, never()).delete(any());
    }

    @Test
    void alreadyFinalizedMovieIsNoOp() {
        when(this.movieRepository.findById(MOVIE_ID)).thenReturn(Mono.empty());

        StepVerifier.create(this.coordinator.process(MOVIE_ID)).verifyComplete();

        verify(this.mediaRepository, never()).findByCatalogItemId(any());
        verify(this.deletionTransaction, never()).finalizeDeletion(any());
    }

    private static CatalogItem movie() {
        return new CatalogItem(
                MOVIE_ID, "Javier", "Dune", CatalogItemStatus.DELETING, EnrichmentStatus.ENRICHED,
                42L, (MovieMetadata) null, CatalogItemVisibility.PRIVATE, Set.of(), MediaKind.MOVIE);
    }

    private static ManagedMediaAsset managedMedia() {
        return new ManagedMediaAsset(
                MediaId.of(11L), MOVIE_ID, 42L, "Javier/videos/dune.mp4", Instant.now());
    }
}
