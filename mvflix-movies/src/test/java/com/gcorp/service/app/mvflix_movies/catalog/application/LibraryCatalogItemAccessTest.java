package com.gcorp.service.app.mvflix_movies.catalog.application;

import static org.mockito.Mockito.when;

import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.EnrichmentStatus;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.MediaKind;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItem;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemAccessDeniedException;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemId;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemRepository;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemStatus;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemVisibility;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Set;

@ExtendWith(MockitoExtension.class)
class LibraryCatalogItemAccessTest {

    @Mock private CatalogItemRepository movieRepository;

    @InjectMocks private LibraryCatalogItemAccess access;

    @Test
    void allowsAccessWhenCatalogItemIsVisibleToUser() {
        CatalogItem movie = movie(CatalogItemVisibility.SHARED, Set.of("Maria"));
        when(this.movieRepository.findById(CatalogItemId.of(10L))).thenReturn(Mono.just(movie));

         StepVerifier.create(this.access.requireVisible(
                         com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemId.of(10L), "Maria"))
                .verifyComplete();
    }

    @Test
    void deniesAccessWhenCatalogItemIsNotVisibleToUser() {
        CatalogItem movie = movie(CatalogItemVisibility.PRIVATE, Set.of());
        when(this.movieRepository.findById(CatalogItemId.of(10L))).thenReturn(Mono.just(movie));

         StepVerifier.create(this.access.requireVisible(
                         com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemId.of(10L), "Maria"))
                .expectError(CatalogItemAccessDeniedException.class)
                .verify();
    }

    @Test
    void doesNotRevealWhetherCatalogItemExists() {
        when(this.movieRepository.findById(CatalogItemId.of(10L))).thenReturn(Mono.empty());

         StepVerifier.create(this.access.requireVisible(
                         com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemId.of(10L), "Maria"))
                .expectError(CatalogItemAccessDeniedException.class)
                .verify();
    }

    private static CatalogItem movie(CatalogItemVisibility visibility, Set<String> sharedWith) {
        return new CatalogItem(
                CatalogItemId.of(10L),
                "Javier",
                "Dune",
                CatalogItemStatus.READY,
                EnrichmentStatus.ENRICHED,
                null,
                null,
                visibility,
                sharedWith,
                MediaKind.MOVIE);
    }
}
