package com.gcorp.service.app.mvflix_movies.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.EnrichmentStatus;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItem;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemId;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemRepository;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemStatus;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemVisibility;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemKind;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class LibraryCatalogItemCreatorTest {

    @Mock private CatalogItemRepository movieRepository;

    @InjectMocks private LibraryCatalogItemCreator creator;

    @Test
    void catalogOwnsHowALibraryItemIsCreated() {
        when(this.movieRepository.save(any(CatalogItem.class)))
                .thenAnswer(invocation -> {
                    CatalogItem movie = invocation.getArgument(0);
                    return Mono.just(new CatalogItem(
                            CatalogItemId.of(50L),
                            movie.getOwnerUsername(),
                            movie.getTitle(),
                            movie.getStatus(),
                            movie.getEnrichmentStatus(),
                            null,
                            movie.getMetadata(),
                            movie.getVisibility(),
                            movie.getSharedWith(),
                            movie.getKind()));
                });

        StepVerifier.create(this.creator.createFromLibrary(
                        "Javier", "Dune", CatalogItemKind.MOVIE))
                 .expectNext(com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemId.of(50L))
                .verifyComplete();

        ArgumentCaptor<CatalogItem> captor = ArgumentCaptor.forClass(CatalogItem.class);
        verify(this.movieRepository).save(captor.capture());
        CatalogItem newMovie = captor.getValue();
        assertThat(newMovie.getOwnerUsername()).isEqualTo("Javier");
        assertThat(newMovie.getTitle()).isEqualTo("Dune");
        assertThat(newMovie.getStatus()).isEqualTo(CatalogItemStatus.READY);
        assertThat(newMovie.getEnrichmentStatus()).isEqualTo(EnrichmentStatus.RAW);
        assertThat(newMovie.getVisibility()).isEqualTo(CatalogItemVisibility.PRIVATE);
        assertThat(newMovie.getKind()).isEqualTo(CatalogItemKind.MOVIE);
    }

    @Test
    void translatesOtherLibraryKindToCatalogClassification() {
        when(this.movieRepository.save(any(CatalogItem.class)))
                .thenAnswer(invocation -> {
                    CatalogItem movie = invocation.getArgument(0);
                    return Mono.just(new CatalogItem(
                            CatalogItemId.of(51L),
                            movie.getOwnerUsername(),
                            movie.getTitle(),
                            movie.getStatus(),
                            movie.getEnrichmentStatus(),
                            null,
                            movie.getMetadata(),
                            movie.getVisibility(),
                            movie.getSharedWith(),
                            movie.getKind()));
                });

        StepVerifier.create(this.creator.createFromLibrary(
                        "Javier", "Live concert", CatalogItemKind.VIDEO))
                 .expectNext(com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemId.of(51L))
                .verifyComplete();

        ArgumentCaptor<CatalogItem> captor = ArgumentCaptor.forClass(CatalogItem.class);
        verify(this.movieRepository).save(captor.capture());
        assertThat(captor.getValue().getKind()).isEqualTo(CatalogItemKind.VIDEO);
    }
}
