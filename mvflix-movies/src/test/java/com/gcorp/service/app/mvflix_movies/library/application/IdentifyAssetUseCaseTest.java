package com.gcorp.service.app.mvflix_movies.library.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gcorp.service.app.mvflix_movies.app.security.AuthenticatedUser;
import com.gcorp.service.app.mvflix_movies.app.security.UserProvider;
import com.gcorp.service.app.mvflix_movies.application.enrichment.EnrichMovieUseCase;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAsset;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAssetAlreadyIdentifiedException;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAssetId;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAssetNotFoundException;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAssetRepository;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAssetStatus;
import com.gcorp.service.app.mvflix_movies.domain.movie.EnrichmentStatus;
import com.gcorp.service.app.mvflix_movies.domain.movie.MediaKind;
import com.gcorp.service.app.mvflix_movies.domain.movie.Movie;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieId;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieStatus;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieVisibility;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

@ExtendWith(MockitoExtension.class)
class IdentifyAssetUseCaseTest {

    @Mock private MediaAssetRepository assetRepository;
    @Mock private UserProvider userProvider;
    @Mock private EnrichMovieUseCase enrichMovieUseCase;
    @Mock private IdentifyAssetTransaction identifyAssetTransaction;

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
                        true,
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
                        java.util.Set.of(), MediaKind.MOVIE);

        when(this.assetRepository.findById(MediaAssetId.of(1L))).thenReturn(Mono.just(asset));
        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("Javier", "j@m.com")));
        when(this.identifyAssetTransaction.execute(
                        eq(asset), eq("Javier"), eq("Dune"), eq(MediaKind.MOVIE)))
                .thenReturn(Mono.just(new IdentificationResult(asset.identify(created.getId()), created)));

        StepVerifier.create(this.useCase.execute(MediaAssetId.of(1L), "Dune", null, null))
                .expectNextMatches(identified ->
                        identified.isIdentified()
                                && identified.getMovieId().equals(MovieId.of(50L)))
                .verifyComplete();

        verify(this.identifyAssetTransaction).execute(
                asset, "Javier", "Dune", MediaKind.MOVIE);
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
                        true,
                        Instant.now(),
                        Instant.now());

        when(this.assetRepository.findById(MediaAssetId.of(1L))).thenReturn(Mono.just(identified));

        StepVerifier.create(this.useCase.execute(MediaAssetId.of(1L), "Dune", null, null))
                .expectNextMatches(asset -> asset.getMovieId().equals(MovieId.of(50L)))
                .verifyComplete();

        verify(this.identifyAssetTransaction, never())
                .execute(
                        any(MediaAsset.class),
                        any(String.class),
                        any(String.class),
                        any(MediaKind.class));
        verify(this.enrichMovieUseCase, never()).enrich(any(Movie.class), any(Long.class));
    }

    @Test
    void unknownAssetFails() {
        when(this.assetRepository.findById(MediaAssetId.of(999L))).thenReturn(Mono.empty());

        StepVerifier.create(this.useCase.execute(MediaAssetId.of(999L), "Dune", null, null))
                .expectError(MediaAssetNotFoundException.class)
                .verify();
    }

    @Test
    void concurrentIdentificationReturnsWinningLink() {
        MediaAsset asset =
                new MediaAsset(
                        MediaAssetId.of(1L),
                        7L,
                        "Dune.mp4",
                        1024,
                        "video/mp4",
                        MediaAssetStatus.UNIDENTIFIED,
                        null,
                        true,
                        Instant.now(),
                        Instant.now());
        MediaAsset winner = asset.identify(MovieId.of(50L));

        when(this.assetRepository.findById(MediaAssetId.of(1L)))
                .thenReturn(Mono.just(asset), Mono.just(winner));
        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("Javier", "j@m.com")));
        when(this.identifyAssetTransaction.execute(
                        eq(asset), eq("Javier"), eq("Dune"), eq(MediaKind.MOVIE)))
                .thenReturn(Mono.error(
                        new MediaAssetAlreadyIdentifiedException("already identified")));

        StepVerifier.create(this.useCase.execute(MediaAssetId.of(1L), "Dune", null, null))
                .expectNext(winner)
                .verifyComplete();
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
                        true,
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
                        java.util.Set.of(), MediaKind.MOVIE);
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
                        java.util.Set.of(), MediaKind.MOVIE);

        when(this.assetRepository.findById(MediaAssetId.of(1L))).thenReturn(Mono.just(asset));
        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("Javier", "j@m.com")));
        when(this.identifyAssetTransaction.execute(
                        eq(asset),
                        eq("Javier"),
                        eq("Interstellar (2014)"),
                        eq(MediaKind.MOVIE)))
                .thenReturn(Mono.just(new IdentificationResult(asset.identify(created.getId()), created)));
        when(this.enrichMovieUseCase.enrich(any(Movie.class), any(Long.class)))
                .thenReturn(Mono.just(enriched));

        StepVerifier.create(this.useCase.execute(MediaAssetId.of(1L), "Interstellar (2014)", 157336L, null))
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
                        true,
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
                        java.util.Set.of(), MediaKind.MOVIE);

        when(this.assetRepository.findById(MediaAssetId.of(1L))).thenReturn(Mono.just(asset));
        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("Javier", "j@m.com")));
        when(this.identifyAssetTransaction.execute(
                        eq(asset), eq("Javier"), eq("Dune"), eq(MediaKind.MOVIE)))
                .thenReturn(Mono.just(new IdentificationResult(asset.identify(created.getId()), created)));
        when(this.enrichMovieUseCase.enrich(any(Movie.class), any(Long.class)))
                .thenReturn(Mono.error(new IllegalStateException("TMDB down")));

        StepVerifier.create(this.useCase.execute(MediaAssetId.of(1L), "Dune", 123L, null))
                .expectNextMatches(identified -> identified.isIdentified())
                .verifyComplete();

        verify(this.identifyAssetTransaction).execute(
                asset, "Javier", "Dune", MediaKind.MOVIE);
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
                        true,
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
                        java.util.Set.of(), MediaKind.MOVIE);

        when(this.assetRepository.findById(MediaAssetId.of(1L))).thenReturn(Mono.just(asset));
        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("Javier", "j@m.com")));
        when(this.identifyAssetTransaction.execute(
                        eq(asset), eq("Javier"), eq("Dune"), eq(MediaKind.MOVIE)))
                .thenReturn(Mono.just(new IdentificationResult(asset.identify(created.getId()), created)));
        when(this.enrichMovieUseCase.enrich(any(Movie.class), any(Long.class)))
                .thenReturn(Mono.never());

        StepVerifier.withVirtualTime(() ->
                        this.useCase.execute(MediaAssetId.of(1L), "Dune", 123L, null))
                .thenAwait(java.time.Duration.ofSeconds(21))
                .expectNextMatches(identified ->
                        identified.isIdentified()
                                && identified.getMovieId().equals(MovieId.of(50L)))
                .verifyComplete();

        verify(this.identifyAssetTransaction).execute(
                asset, "Javier", "Dune", MediaKind.MOVIE);
    }
}
