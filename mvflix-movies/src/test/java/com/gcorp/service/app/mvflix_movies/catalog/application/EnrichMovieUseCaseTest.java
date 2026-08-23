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
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MediaKind;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.Movie;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieId;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieMetadata;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieNotFoundException;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieRepository;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieStatus;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieVisibility;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Set;

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
                    EnrichmentStatus.RAW, null, RAW_METADATA, MovieVisibility.PRIVATE, Set.of(), MediaKind.MOVIE);

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
        when(this.movieRepository.updateEnrichment(any(Movie.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(this.useCase.enrichCurrentUser(MovieId.of(1L)))
                .assertNext(movie -> assertThat(movie.getEnrichmentStatus())
                        .isEqualTo(EnrichmentStatus.ENRICHED))
                .verifyComplete();

        verify(this.metadataSource).search("The Colossus of Rhodes", null);
        verify(this.metadataSource).findById(274_003L);
        verify(this.movieRepository).updateEnrichment(any(Movie.class));
    }

    @Test
    void enrichSkipsSearchWhenTmdbIdAlreadyPersisted() {
        MovieMetadata withTmdbId = new MovieMetadata(
                "The Colossus of Rhodes", null, null, null, null, null, null, null, null,
                null, null, null, null, null, 274_003L);
        Movie movie = new Movie(
                MovieId.of(2L), "pepe", "The Colossus of Rhodes", MovieStatus.DRAFT,
                EnrichmentStatus.RAW, null, withTmdbId, MovieVisibility.PRIVATE, Set.of(), MediaKind.MOVIE);

        when(this.metadataSource.findById(274_003L)).thenReturn(Mono.just(TMDB_DETAIL));
        when(this.movieRepository.updateEnrichment(any(Movie.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(this.useCase.enrich(movie))
                .assertNext(enriched -> assertThat(enriched.getEnrichmentStatus())
                        .isEqualTo(EnrichmentStatus.ENRICHED))
                .verifyComplete();

        verify(this.metadataSource, never()).search(any(), any());
        verify(this.movieRepository).updateEnrichment(any(Movie.class));
    }

    @Test
    void enrichWithExplicitTmdbIdSkipsSearchAndUsesChosenCandidate() {
        when(this.metadataSource.findById(43020L)).thenReturn(Mono.just(TMDB_DETAIL));
        when(this.movieRepository.updateEnrichment(any(Movie.class)))
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
                    assertThat(movie.getMetadata().tmdbId()).isNull();
                })
                .verifyComplete();

        verify(this.movieRepository, never()).updateEnrichment(any(Movie.class));
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
    void unlinkCurrentUserPersistsAggregateTransition() {
        Movie linked = DRAFT_RAW.linkProviderMetadata(mergedMetadata());
        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("pepe", "sub-1")));
        when(this.movieRepository.findById(MovieId.of(1L))).thenReturn(Mono.just(linked));
        when(this.movieRepository.updateEnrichment(any(Movie.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(this.useCase.unlinkCurrentUser(MovieId.of(1L)))
                .assertNext(movie -> {
                    assertThat(movie.getEnrichmentStatus()).isEqualTo(EnrichmentStatus.RAW);
                    assertThat(movie.getMetadata().tmdbId()).isNull();
                    assertThat(movie.getMetadata().posterPath()).isNull();
                })
                .verifyComplete();

        ArgumentCaptor<Movie> captor = ArgumentCaptor.forClass(Movie.class);
        verify(this.movieRepository).updateEnrichment(captor.capture());
        assertThat(captor.getValue().getEnrichmentStatus()).isEqualTo(EnrichmentStatus.RAW);
    }

    @Test
    void enrichAlreadyEnrichedIsNoOp() {
        Movie enriched = DRAFT_RAW.linkProviderMetadata(mergedMetadata());

        StepVerifier.create(this.useCase.enrich(enriched))
                .expectNext(enriched)
                .verifyComplete();

        verifyNoInteractions(this.metadataSource);
        verify(this.movieRepository, never()).updateEnrichment(any(Movie.class));
    }

    @Test
    void reMatchReplacesMetadataCompletely() {
        MovieMetadata old = new MovieMetadata(
                "Old", "Old", 2000, List.of("Action"), 5.0, "1h 30m", "Old Dir",
                List.of("Old Actor"), "old overview", "/old.jpg", "2000-01-01", "USA", "en",
                List.of("Old Award"), 274_003L);
        Movie movie = new Movie(
                MovieId.of(3L), "pepe", "Old", MovieStatus.READY,
                EnrichmentStatus.ENRICHED, null, old, MovieVisibility.PRIVATE, Set.of(),
                MediaKind.MOVIE);

        ExternalMovieDetail fresh = new ExternalMovieDetail(
                43020L, "New", "New", 2021, List.of("Sci-Fi"), 8.0, 120, null,
                List.of(), null, "/new.jpg", "2021-01-01", null, null);

        when(this.metadataSource.findById(43020L)).thenReturn(Mono.just(fresh));
        when(this.movieRepository.updateEnrichment(any(Movie.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(this.useCase.enrich(movie, 43020L))
                .assertNext(m -> assertThat(m.getEnrichmentStatus())
                        .isEqualTo(EnrichmentStatus.ENRICHED))
                .verifyComplete();

        ArgumentCaptor<Movie> captor = ArgumentCaptor.forClass(Movie.class);
        verify(this.movieRepository).updateEnrichment(captor.capture());
        MovieMetadata result = captor.getValue().getMetadata();
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

        verify(this.movieRepository, never()).updateEnrichment(any(Movie.class));
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
