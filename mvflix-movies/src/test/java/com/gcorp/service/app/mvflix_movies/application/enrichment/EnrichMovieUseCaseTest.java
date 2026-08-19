package com.gcorp.service.app.mvflix_movies.application.enrichment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.gcorp.service.app.mvflix_movies.app.security.AuthenticatedUser;
import com.gcorp.service.app.mvflix_movies.app.security.UserProvider;
import com.gcorp.service.app.mvflix_movies.domain.enrichment.ExternalMovieDetail;
import com.gcorp.service.app.mvflix_movies.domain.enrichment.ExternalMovieSearch;
import com.gcorp.service.app.mvflix_movies.domain.enrichment.MetadataSource;
import com.gcorp.service.app.mvflix_movies.domain.movie.EnrichmentStatus;
import com.gcorp.service.app.mvflix_movies.domain.movie.Movie;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieId;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieMetadata;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieNotFoundException;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieRepository;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieStatus;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieVisibility;

import org.junit.jupiter.api.Test;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

class EnrichMovieUseCaseTest {

    private final MovieRepository movieRepository = mock(MovieRepository.class);
    private final MetadataSource metadataSource = mock(MetadataSource.class);
    private final UserProvider userProvider = mock(UserProvider.class);
    private final EnrichMovieUseCase useCase =
            new EnrichMovieUseCase(movieRepository, metadataSource, userProvider);

    private static final MovieMetadata RAW_METADATA =
            new MovieMetadata(
                    "The Colossus of Rhodes", null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null);

    private static final Movie DRAFT_RAW =
            new Movie(
                    MovieId.of(1L), "pepe", "The Colossus of Rhodes", MovieStatus.DRAFT,
                    EnrichmentStatus.RAW, null, RAW_METADATA, MovieVisibility.PRIVATE);

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
        when(this.movieRepository.findById(MovieId.of(1L))).thenReturn(Mono.just(DRAFT_RAW));
        when(this.metadataSource.search("The Colossus of Rhodes", null))
                .thenReturn(Mono.just(new ExternalMovieSearch(
                        274_003L, "Il colosso di Rodi", 1961, null, "1961-06-20", "Overview...")));
        when(this.metadataSource.findById(274_003L)).thenReturn(Mono.just(TMDB_DETAIL));
        when(this.movieRepository.updateEnrichment(
                        eq(MovieId.of(1L)), eq("pepe"), any(MovieMetadata.class),
                        eq(EnrichmentStatus.ENRICHED)))
                .thenReturn(Mono.just(DRAFT_RAW.applyEnrichment(
                        mergedMetadata(), EnrichmentStatus.ENRICHED)));

        StepVerifier.create(this.useCase.enrichCurrentUser(MovieId.of(1L)))
                .assertNext(movie -> assertThat(movie.getEnrichmentStatus())
                        .isEqualTo(EnrichmentStatus.ENRICHED))
                .verifyComplete();

        verify(this.metadataSource).search("The Colossus of Rhodes", null);
        verify(this.metadataSource).findById(274_003L);
        verify(this.movieRepository).updateEnrichment(
                eq(MovieId.of(1L)), eq("pepe"), any(MovieMetadata.class),
                eq(EnrichmentStatus.ENRICHED));
    }

    @Test
    void enrichSkipsSearchWhenTmdbIdAlreadyPersisted() {
        MovieMetadata withTmdbId = new MovieMetadata(
                "The Colossus of Rhodes", null, null, null, null, null, null, null, null,
                null, null, null, null, null, 274_003L);
        Movie movie = new Movie(
                MovieId.of(2L), "pepe", "The Colossus of Rhodes", MovieStatus.DRAFT,
                EnrichmentStatus.RAW, null, withTmdbId, MovieVisibility.PRIVATE);

        when(this.metadataSource.findById(274_003L)).thenReturn(Mono.just(TMDB_DETAIL));
        when(this.movieRepository.updateEnrichment(
                        eq(MovieId.of(2L)), eq("pepe"), any(MovieMetadata.class),
                        eq(EnrichmentStatus.ENRICHED)))
                .thenReturn(Mono.just(movie.applyEnrichment(
                        mergedMetadata(), EnrichmentStatus.ENRICHED)));

        StepVerifier.create(this.useCase.enrich(movie))
                .assertNext(enriched -> assertThat(enriched.getEnrichmentStatus())
                        .isEqualTo(EnrichmentStatus.ENRICHED))
                .verifyComplete();

        verify(this.metadataSource, never()).search(any(), any());
        verify(this.movieRepository).updateEnrichment(
                eq(MovieId.of(2L)), eq("pepe"), any(MovieMetadata.class),
                eq(EnrichmentStatus.ENRICHED));
    }

    @Test
    void enrichWithExplicitTmdbIdSkipsSearchAndUsesChosenCandidate() {
        when(this.metadataSource.findById(43020L)).thenReturn(Mono.just(TMDB_DETAIL));
        when(this.movieRepository.updateEnrichment(
                        eq(MovieId.of(1L)), eq("pepe"), any(MovieMetadata.class),
                        eq(EnrichmentStatus.ENRICHED)))
                .thenReturn(Mono.just(DRAFT_RAW.applyEnrichment(
                        mergedMetadata(), EnrichmentStatus.ENRICHED)));

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
                    assertThat(movie.getMetadata().tmdbId()).isNull();
                })
                .verifyComplete();

        verify(this.movieRepository, never()).updateEnrichment(
                any(), any(), any(), any());
    }

    @Test
    void enrichCurrentUserRejectsForeignOwner() {
        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("otro", "sub-2")));
        when(this.movieRepository.findById(MovieId.of(1L))).thenReturn(Mono.just(DRAFT_RAW));

        StepVerifier.create(this.useCase.enrichCurrentUser(MovieId.of(1L)))
                .expectError(MovieNotFoundException.class)
                .verify();

        verifyNoInteractions(this.metadataSource);
    }

    @Test
    void enrichAlreadyEnrichedIsNoOp() {
        Movie enriched = DRAFT_RAW.applyEnrichment(mergedMetadata(), EnrichmentStatus.ENRICHED);

        StepVerifier.create(this.useCase.enrich(enriched))
                .expectNext(enriched)
                .verifyComplete();

        verifyNoInteractions(this.metadataSource);
        verify(this.movieRepository, never()).updateEnrichment(any(), any(), any(), any());
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