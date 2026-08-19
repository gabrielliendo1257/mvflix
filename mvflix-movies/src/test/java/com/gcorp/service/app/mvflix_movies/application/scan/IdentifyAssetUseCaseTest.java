package com.gcorp.service.app.mvflix_movies.application.scan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gcorp.service.app.mvflix_movies.app.security.AuthenticatedUser;
import com.gcorp.service.app.mvflix_movies.app.security.UserProvider;
import com.gcorp.service.app.mvflix_movies.application.enrichment.EnrichMovieUseCase;
import com.gcorp.service.app.mvflix_movies.domain.mediaasset.MediaAsset;
import com.gcorp.service.app.mvflix_movies.domain.mediaasset.MediaAssetId;
import com.gcorp.service.app.mvflix_movies.domain.mediaasset.MediaAssetNotFoundException;
import com.gcorp.service.app.mvflix_movies.domain.mediaasset.MediaAssetRepository;
import com.gcorp.service.app.mvflix_movies.domain.mediaasset.MediaAssetStatus;
import com.gcorp.service.app.mvflix_movies.domain.movie.EnrichmentStatus;
import com.gcorp.service.app.mvflix_movies.domain.movie.Movie;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieId;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieRepository;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieStatus;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieVisibility;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

@ExtendWith(MockitoExtension.class)
class IdentifyAssetUseCaseTest {

    @Mock private MediaAssetRepository assetRepository;
    @Mock private MovieRepository movieRepository;
    @Mock private UserProvider userProvider;
    @Mock private EnrichMovieUseCase enrichMovieUseCase;

    @InjectMocks private IdentifyAssetUseCase useCase;

    @Test
    void linksAssetToNewReadyMovie() {
        MediaAsset asset =
                new MediaAsset(
                        MediaAssetId.of(1L),
                        7L,
                        "Dune.mp4",
                        1024,
                        "video/mp4",
                        MediaAssetStatus.UNIDENTIFIED,
                        null,
                        Instant.now(),
                        Instant.now());
        Movie created =
                new Movie(
                        MovieId.of(50L),
                        "Javier",
                        "Dune",
                        MovieStatus.READY,
                        EnrichmentStatus.RAW,
                        null,
                        null,
                        MovieVisibility.PRIVATE,
                        java.util.Set.of());

        when(this.assetRepository.findById(MediaAssetId.of(1L))).thenReturn(Mono.just(asset));
        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("Javier", "j@m.com")));
        when(this.movieRepository.save(any(Movie.class))).thenReturn(Mono.just(created));
        when(this.assetRepository.save(any(MediaAsset.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        ArgumentCaptor<Movie> movieCaptor = ArgumentCaptor.forClass(Movie.class);

        StepVerifier.create(this.useCase.execute(MediaAssetId.of(1L), "Dune", null))
                .expectNextMatches(identified ->
                        identified.isIdentified()
                                && identified.getMovieId().equals(MovieId.of(50L)))
                .verifyComplete();

        verify(this.movieRepository).save(movieCaptor.capture());
        Movie saved = movieCaptor.getValue();
        assertThat(saved.getTitle()).isEqualTo("Dune");
        assertThat(saved.getStatus()).isEqualTo(MovieStatus.READY);
        assertThat(saved.getEnrichmentStatus()).isEqualTo(EnrichmentStatus.RAW);
        assertThat(saved.getOwnerUsername()).isEqualTo("Javier");
        verify(this.enrichMovieUseCase, never()).enrich(any(Movie.class), any(Long.class));
    }

    @Test
    void alreadyIdentifiedAssetIsIdempotent() {
        MediaAsset identified =
                new MediaAsset(
                        MediaAssetId.of(1L),
                        7L,
                        "Dune.mp4",
                        1024,
                        "video/mp4",
                        MediaAssetStatus.IDENTIFIED,
                        MovieId.of(50L),
                        Instant.now(),
                        Instant.now());

        when(this.assetRepository.findById(MediaAssetId.of(1L))).thenReturn(Mono.just(identified));

        StepVerifier.create(this.useCase.execute(MediaAssetId.of(1L), "Dune", null))
                .expectNextMatches(asset -> asset.getMovieId().equals(MovieId.of(50L)))
                .verifyComplete();

        verify(this.movieRepository, never()).save(any(Movie.class));
        verify(this.assetRepository, never()).save(any(MediaAsset.class));
        verify(this.enrichMovieUseCase, never()).enrich(any(Movie.class), any(Long.class));
    }

    @Test
    void unknownAssetFails() {
        when(this.assetRepository.findById(MediaAssetId.of(999L))).thenReturn(Mono.empty());

        StepVerifier.create(this.useCase.execute(MediaAssetId.of(999L), "Dune", null))
                .expectError(MediaAssetNotFoundException.class)
                .verify();
    }

    @Test
    void linksAndAutocompletesWithTmdbInOneStep() {
        MediaAsset asset =
                new MediaAsset(
                        MediaAssetId.of(1L),
                        7L,
                        "Interstellar (2014).mkv",
                        1024,
                        "video/x-matroska",
                        MediaAssetStatus.UNIDENTIFIED,
                        null,
                        Instant.now(),
                        Instant.now());
        Movie created =
                new Movie(
                        MovieId.of(50L),
                        "Javier",
                        "Interstellar (2014)",
                        MovieStatus.READY,
                        EnrichmentStatus.RAW,
                        null,
                        null,
                        MovieVisibility.PRIVATE,
                        java.util.Set.of());
        Movie enriched =
                new Movie(
                        MovieId.of(50L),
                        "Javier",
                        "Interstellar",
                        MovieStatus.READY,
                        EnrichmentStatus.ENRICHED,
                        null,
                        null,
                        MovieVisibility.PRIVATE,
                        java.util.Set.of());

        when(this.assetRepository.findById(MediaAssetId.of(1L))).thenReturn(Mono.just(asset));
        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("Javier", "j@m.com")));
        when(this.movieRepository.save(any(Movie.class))).thenReturn(Mono.just(created));
        when(this.enrichMovieUseCase.enrich(any(Movie.class), any(Long.class)))
                .thenReturn(Mono.just(enriched));
        when(this.assetRepository.save(any(MediaAsset.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(this.useCase.execute(MediaAssetId.of(1L), "Interstellar (2014)", 157336L))
                .expectNextMatches(identified ->
                        identified.isIdentified()
                                && identified.getMovieId().equals(MovieId.of(50L)))
                .verifyComplete();

        verify(this.enrichMovieUseCase).enrich(any(Movie.class), eq(157336L));
    }

    @Test
    void tmdbFailureDoesNotBreakIdentification() {
        MediaAsset asset =
                new MediaAsset(
                        MediaAssetId.of(1L),
                        7L,
                        "Dune.mp4",
                        1024,
                        "video/mp4",
                        MediaAssetStatus.UNIDENTIFIED,
                        null,
                        Instant.now(),
                        Instant.now());
        Movie created =
                new Movie(
                        MovieId.of(50L),
                        "Javier",
                        "Dune",
                        MovieStatus.READY,
                        EnrichmentStatus.RAW,
                        null,
                        null,
                        MovieVisibility.PRIVATE,
                        java.util.Set.of());

        when(this.assetRepository.findById(MediaAssetId.of(1L))).thenReturn(Mono.just(asset));
        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("Javier", "j@m.com")));
        when(this.movieRepository.save(any(Movie.class))).thenReturn(Mono.just(created));
        when(this.enrichMovieUseCase.enrich(any(Movie.class), any(Long.class)))
                .thenReturn(Mono.error(new IllegalStateException("TMDB down")));
        when(this.assetRepository.save(any(MediaAsset.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(this.useCase.execute(MediaAssetId.of(1L), "Dune", 123L))
                .expectNextMatches(identified -> identified.isIdentified())
                .verifyComplete();

        verify(this.assetRepository).save(any(MediaAsset.class));
    }

    @Test
    void slowTmdbDoesNotBreakIdentification() {
        MediaAsset asset =
                new MediaAsset(
                        MediaAssetId.of(1L),
                        7L,
                        "Dune.mp4",
                        1024,
                        "video/mp4",
                        MediaAssetStatus.UNIDENTIFIED,
                        null,
                        Instant.now(),
                        Instant.now());
        Movie created =
                new Movie(
                        MovieId.of(50L),
                        "Javier",
                        "Dune",
                        MovieStatus.READY,
                        EnrichmentStatus.RAW,
                        null,
                        null,
                        MovieVisibility.PRIVATE,
                        java.util.Set.of());

        when(this.assetRepository.findById(MediaAssetId.of(1L))).thenReturn(Mono.just(asset));
        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("Javier", "j@m.com")));
        when(this.movieRepository.save(any(Movie.class))).thenReturn(Mono.just(created));
        when(this.enrichMovieUseCase.enrich(any(Movie.class), any(Long.class)))
                .thenReturn(Mono.never());
        when(this.assetRepository.save(any(MediaAsset.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.withVirtualTime(() ->
                        this.useCase.execute(MediaAssetId.of(1L), "Dune", 123L))
                .thenAwait(java.time.Duration.ofSeconds(21))
                .expectNextMatches(identified ->
                        identified.isIdentified()
                                && identified.getMovieId().equals(MovieId.of(50L)))
                .verifyComplete();

        verify(this.assetRepository).save(any(MediaAsset.class));
    }
}
