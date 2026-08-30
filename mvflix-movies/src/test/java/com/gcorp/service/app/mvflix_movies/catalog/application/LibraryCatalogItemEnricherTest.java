package com.gcorp.service.app.mvflix_movies.catalog.application;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.EnrichmentStatus;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MediaKind;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItem;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItemId;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItemNotFoundException;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItemRepository;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItemStatus;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItemVisibility;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Set;

@ExtendWith(MockitoExtension.class)
class LibraryCatalogItemEnricherTest {

    @Mock private CatalogItemRepository movieRepository;
    @Mock private EnrichMovieUseCase enrichMovieUseCase;

    @InjectMocks private LibraryCatalogItemEnricher enricher;

    @Test
    void enrichesTheCatalogItemWithoutExposingCatalogInternalsToLibrary() {
        CatalogItem movie = movie();
         when(this.movieRepository.findById(CatalogItemId.of(50L))).thenReturn(Mono.just(movie));
        when(this.enrichMovieUseCase.enrich(movie, 123L)).thenReturn(Mono.just(movie));

         StepVerifier.create(this.enricher.enrich(
                         com.gcorp.service.app.mvflix_movies.library.domain.CatalogItemId.of(50L), 123L))
                .verifyComplete();

        verify(this.enrichMovieUseCase).enrich(movie, 123L);
    }

    @Test
    void reportsWhenTheCreatedCatalogItemCannotBeReloaded() {
        when(this.movieRepository.findById(CatalogItemId.of(50L))).thenReturn(Mono.empty());

         StepVerifier.create(this.enricher.enrich(
                         com.gcorp.service.app.mvflix_movies.library.domain.CatalogItemId.of(50L), 123L))
                .expectError(CatalogItemNotFoundException.class)
                .verify();
    }

    private static CatalogItem movie() {
        return new CatalogItem(
                CatalogItemId.of(50L),
                "Javier",
                "Dune",
                CatalogItemStatus.READY,
                EnrichmentStatus.RAW,
                null,
                null,
                CatalogItemVisibility.PRIVATE,
                Set.of(),
                MediaKind.MOVIE);
    }
}
