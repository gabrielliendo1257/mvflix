package com.gcorp.service.app.mvflix_movies.catalog.infrastructure.scheduler;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gcorp.service.app.mvflix_movies.catalog.application.CatalogItemDeletionTransaction;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.EnrichmentStatus;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemKind;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItem;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemId;
import com.gcorp.service.app.mvflix_movies.catalog.domain.metadata.MovieMetadata;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemRepository;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemStatus;
import com.gcorp.service.app.mvflix_movies.catalog.domain.access.Visibility;

import org.junit.jupiter.api.Test;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.time.Duration;

class DeletionRecoveryJobTest {

    private final CatalogItemRepository movieRepository = org.mockito.Mockito.mock(CatalogItemRepository.class);
    private final CatalogItemDeletionTransaction transaction = org.mockito.Mockito.mock(CatalogItemDeletionTransaction.class);

    @Test
    void ensuresDeletingMoviesAndReactivatesExhaustedOutbox() {
        CatalogItem movie = new CatalogItem(CatalogItemId.of(7L), "pepe", "Dune", CatalogItemStatus.DELETING,
                EnrichmentStatus.ENRICHED, null, (MovieMetadata) null, Visibility.PRIVATE,
                Set.of(), CatalogItemKind.MOVIE);
        when(movieRepository.findDeletingForRecovery(25, Duration.ofMinutes(1))).thenReturn(Flux.just(movie));
        when(movieRepository.markRecoveryAttempt(movie.getId())).thenReturn(Mono.empty());
        when(transaction.ensureDeletionRequested(movie.getId())).thenReturn(Mono.empty());

        new DeletionRecoveryJob(movieRepository, transaction, 25, Duration.ofMinutes(1))
                .recoverBatch().block();

        verify(transaction).ensureDeletionRequested(movie.getId());
    }
}
