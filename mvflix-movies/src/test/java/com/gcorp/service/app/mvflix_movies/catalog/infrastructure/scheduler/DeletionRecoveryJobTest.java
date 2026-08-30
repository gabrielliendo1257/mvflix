package com.gcorp.service.app.mvflix_movies.catalog.infrastructure.scheduler;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gcorp.service.app.mvflix_movies.catalog.application.MovieDeletionTransaction;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.EnrichmentStatus;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MediaKind;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItem;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItemId;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieMetadata;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItemRepository;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItemStatus;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItemVisibility;

import org.junit.jupiter.api.Test;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.time.Duration;

class DeletionRecoveryJobTest {

    private final CatalogItemRepository movieRepository = org.mockito.Mockito.mock(CatalogItemRepository.class);
    private final MovieDeletionTransaction transaction = org.mockito.Mockito.mock(MovieDeletionTransaction.class);

    @Test
    void ensuresDeletingMoviesAndReactivatesExhaustedOutbox() {
        CatalogItem movie = new CatalogItem(CatalogItemId.of(7L), "pepe", "Dune", CatalogItemStatus.DELETING,
                EnrichmentStatus.ENRICHED, null, (MovieMetadata) null, CatalogItemVisibility.PRIVATE,
                Set.of(), MediaKind.MOVIE);
        when(movieRepository.findDeletingForRecovery(25, Duration.ofMinutes(1))).thenReturn(Flux.just(movie));
        when(movieRepository.markRecoveryAttempt(movie.getId())).thenReturn(Mono.empty());
        when(transaction.ensureDeletionRequested(movie.getId())).thenReturn(Mono.empty());

        new DeletionRecoveryJob(movieRepository, transaction, 25, Duration.ofMinutes(1))
                .recoverBatch().block();

        verify(transaction).ensureDeletionRequested(movie.getId());
    }
}
