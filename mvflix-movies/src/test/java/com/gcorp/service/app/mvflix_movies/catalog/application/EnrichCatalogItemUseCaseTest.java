package com.gcorp.service.app.mvflix_movies.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.gcorp.service.app.mvflix_movies.shared.application.security.AuthenticatedUser;
import com.gcorp.service.app.mvflix_movies.shared.application.security.UserProvider;
import com.gcorp.service.app.mvflix_movies.catalog.application.port.ExternalMovieDetail;
import com.gcorp.service.app.mvflix_movies.catalog.application.port.ExternalMovieSearch;
import com.gcorp.service.app.mvflix_movies.catalog.application.port.MetadataSource;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.EnrichmentStatus;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemKind;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItem;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemId;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieMetadata;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemNotFoundException;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemRepository;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemStatus;
import com.gcorp.service.app.mvflix_movies.catalog.domain.access.Visibility;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Set;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

class EnrichCatalogItemUseCaseTest {

    private final CatalogItemRepository movieRepository = mock(CatalogItemRepository.class);
    private final MetadataSource metadataSource = mock(MetadataSource.class);
    private final UserProvider userProvider = mock(UserProvider.class);
    private final EnrichCatalogItemUseCase useCase =
            new EnrichCatalogItemUseCase(movieRepository, metadataSource, userProvider);

    private static final MovieMetadata RAW_METADATA =
            new MovieMetadata(
                    "The Colossus of Rhodes", null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null);

    private static final CatalogItem DRAFT_RAW =
            new CatalogItem(
                    CatalogItemId.of(1L), "pepe", "The Colossus of Rhodes", CatalogItemStatus.DRAFT,
                    EnrichmentStatus.RAW, null, RAW_METADATA, Visibility.PRIVATE, Set.of(), CatalogItemKind.MOVIE);

    private static final ExternalMovieDetail TMDB_DETAIL =
            new ExternalMovieDetail(
                    274_003L, "Il colosso di Rodi", "Il colosso di Rodi", 1961,
                    List.of("Adventure", "Action"), 3.2, 128, "Sergio Leone",
                    List.of("Rory Calhoun", "Lea Massari", "Georges Marchal"), "Overview...",
                    "https://image.tmdb.org/t/p/w500/xYZ.jpg", "1961-06-20", "Italy", "it");

    @Test
    void enrichCurrentUserMatchesByTitleAndPersistsEnrichedMetadata() {
        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("pepe", "sub-1")));
        when(this.movieRepository.findById(CatalogItemId.of(1L))).thenReturn(Mono.just(DRAFT_RAW));
        when(this.metadataSource.search("The Colossus of Rhodes", null))
                .thenReturn(Mono.just(new ExternalMovieSearch(
                        274_003L, "Il colosso di Rodi", 1961, null, "1961-06-20", "Overview...")));
        when(this.metadataSource.findById(274_003L)).thenReturn(Mono.just(TMDB_DETAIL));
        when(this.movieRepository.updateEnrichment(any(CatalogItem.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(this.useCase.enrichCurrentUser(CatalogItemId.of(1L)))
                .assertNext(movie -> assertThat(movie.getEnrichmentStatus())
                        .isEqualTo(EnrichmentStatus.ENRICHED))
                .verifyComplete();

        verify(this.metadataSource).search("The Colossus of Rhodes", null);
        verify(this.metadataSource).findById(274_003L);
        verify(this.movieRepository).updateEnrichment(any(CatalogItem.class));
    }

    @Test
    void enrichSkipsSearchWhenTmdbIdAlreadyPersisted() {
        MovieMetadata withTmdbId = new MovieMetadata(
                "The Colossus of Rhodes", null, null, null, null, null, null, null, null,
                null, null, null, null, null, 274_003L);
        CatalogItem movie = new CatalogItem(
                CatalogItemId.of(2L), "pepe", "The Colossus of Rhodes", CatalogItemStatus.DRAFT,
                    EnrichmentStatus.RAW, null, withTmdbId, Visibility.PRIVATE, Set.of(), CatalogItemKind.MOVIE);

        when(this.metadataSource.findById(274_003L)).thenReturn(Mono.just(TMDB_DETAIL));
        when(this.movieRepository.updateEnrichment(any(CatalogItem.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(this.useCase.enrich(movie))
                .assertNext(enriched -> assertThat(enriched.getEnrichmentStatus())
                        .isEqualTo(EnrichmentStatus.ENRICHED))
                .verifyComplete();

        verify(this.metadataSource, never()).search(any(), any());
        verify(this.movieRepository).updateEnrichment(any(CatalogItem.class));
    }

    @Test
    void enrichWithExplicitTmdbIdSkipsSearchAndUsesChosenCandidate() {
        when(this.metadataSource.findById(43020L)).thenReturn(Mono.just(TMDB_DETAIL));
        when(this.movieRepository.updateEnrichment(any(CatalogItem.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(this.useCase.enrich(DRAFT_RAW, 43020L))
                .assertNext(movie -> assertThat(movie.getEnrichmentStatus())
                        .isEqualTo(EnrichmentStatus.ENRICHED))
                .verifyComplete();

        verify(this.metadataSource).findById(43020L);
        verify(this.metadataSource, never()).search(any(), any());
    }

    @Test
    void enrichWithoutMatchLeavesMovieRaw() {
        when(this.metadataSource.search("The Colossus of Rhodes", null))
                .thenReturn(Mono.empty());

        StepVerifier.create(this.useCase.enrich(DRAFT_RAW))
                .assertNext(movie -> {
                    assertThat(movie.getEnrichmentStatus()).isEqualTo(EnrichmentStatus.RAW);
                    assertThat(movie.getMovieMetadata().tmdbId()).isNull();
                })
                .verifyComplete();

        verify(this.movieRepository, never()).updateEnrichment(any(CatalogItem.class));
    }

    @Test
    void enrichCurrentUserRejectsForeignOwner() {
        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("otro", "sub-2")));
        when(this.movieRepository.findById(CatalogItemId.of(1L))).thenReturn(Mono.just(DRAFT_RAW));

        StepVerifier.create(this.useCase.enrichCurrentUser(CatalogItemId.of(1L)))
                .expectError(CatalogItemNotFoundException.class)
                .verify();

        verifyNoInteractions(this.metadataSource);
    }

    @Test
    void unlinkCurrentUserPersistsAggregateTransition() {
        CatalogItem linked = DRAFT_RAW.linkProviderMetadata(mergedMetadata());
        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("pepe", "sub-1")));
        when(this.movieRepository.findById(CatalogItemId.of(1L))).thenReturn(Mono.just(linked));
        when(this.movieRepository.updateEnrichment(any(CatalogItem.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(this.useCase.unlinkCurrentUser(CatalogItemId.of(1L)))
                .assertNext(movie -> {
                    assertThat(movie.getEnrichmentStatus()).isEqualTo(EnrichmentStatus.RAW);
                    assertThat(movie.getMovieMetadata().tmdbId()).isNull();
                    assertThat(movie.getMovieMetadata().posterPath()).isNull();
                })
                .verifyComplete();

        ArgumentCaptor<CatalogItem> captor = ArgumentCaptor.forClass(CatalogItem.class);
        verify(this.movieRepository).updateEnrichment(captor.capture());
        assertThat(captor.getValue().getEnrichmentStatus()).isEqualTo(EnrichmentStatus.RAW);
    }

    @Test
    void enrichAlreadyEnrichedIsNoOp() {
        CatalogItem enriched = DRAFT_RAW.linkProviderMetadata(mergedMetadata());

        StepVerifier.create(this.useCase.enrich(enriched))
                .expectNext(enriched)
                .verifyComplete();

        verifyNoInteractions(this.metadataSource);
        verify(this.movieRepository, never()).updateEnrichment(any(CatalogItem.class));
    }

    @Test
    void reMatchReplacesMetadataCompletely() {
        MovieMetadata old = new MovieMetadata(
                "Old", "Old", 2000, List.of("Action"), 5.0, "1h 30m", "Old Dir",
                List.of("Old Actor"), "old overview", "/old.jpg", "2000-01-01", "USA", "en",
                List.of("Old Award"), 274_003L);
        CatalogItem movie = new CatalogItem(
                CatalogItemId.of(3L), "pepe", "Old", CatalogItemStatus.READY,
                EnrichmentStatus.ENRICHED, null, old, Visibility.PRIVATE, Set.of(),
                CatalogItemKind.MOVIE);

        ExternalMovieDetail fresh = new ExternalMovieDetail(
                43020L, "New", "New", 2021, List.of("Sci-Fi"), 8.0, 120, null,
                List.of(), null, "/new.jpg", "2021-01-01", null, null);

        when(this.metadataSource.findById(43020L)).thenReturn(Mono.just(fresh));
        when(this.movieRepository.updateEnrichment(any(CatalogItem.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(this.useCase.enrich(movie, 43020L))
                .assertNext(m -> assertThat(m.getEnrichmentStatus())
                        .isEqualTo(EnrichmentStatus.ENRICHED))
                .verifyComplete();

        ArgumentCaptor<CatalogItem> captor = ArgumentCaptor.forClass(CatalogItem.class);
        verify(this.movieRepository).updateEnrichment(captor.capture());
        MovieMetadata result = captor.getValue().getMovieMetadata();
        assertThat(result.tmdbId()).isEqualTo(43020L);
        assertThat(result.title()).isEqualTo("New");
        assertThat(result.director()).isNull();
        assertThat(result.overview()).isNull();
        assertThat(result.country()).isNull();
        assertThat(result.awards()).isNull();
        assertThat(result.posterPath()).isEqualTo("/new.jpg");
    }

    @Test
    void synchronousSourceFailureBecomesMonoError() {
        when(this.metadataSource.findById(274_003L))
                .thenThrow(new IllegalStateException("TMDB_API_TOKEN no configurado"));

        StepVerifier.create(this.useCase.enrich(DRAFT_RAW, 274_003L))
                .expectError(IllegalStateException.class)
                .verify();

        verify(this.movieRepository, never()).updateEnrichment(any(CatalogItem.class));
    }

    private static MovieMetadata mergedMetadata() {
        return new MovieMetadata(
                "Il colosso di Rodi", "Il colosso di Rodi", 1961,
                List.of("Adventure", "Action"), 3.2, "2h 8m", "Sergio Leone",
                List.of("Rory Calhoun", "Lea Massari", "Georges Marchal"), "Overview...",
                "https://image.tmdb.org/t/p/w500/xYZ.jpg", "1961-06-20", "Italy", "it",
                null, 274_003L);
    }
}
