package com.gcorp.service.app.mvflix_movies.library.application;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gcorp.service.app.mvflix_movies.app.security.AuthenticatedUser;
import com.gcorp.service.app.mvflix_movies.app.security.UserProvider;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAsset;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAssetId;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAssetNotFoundException;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAssetRepository;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAssetStatus;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieAccessDeniedException;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieId;
import com.gcorp.service.app.mvflix_movies.library.application.port.CatalogItemAccess;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
@ExtendWith(MockitoExtension.class)
class MediaAssetQueriesTest {

    @Mock private MediaAssetRepository assetRepository;
    @Mock private CatalogItemAccess catalogItemAccess;
    @Mock private UserProvider userProvider;

    @InjectMocks private MediaAssetQueries queries;

    @Test
    void listsAllAssetsWhenStatusIsAbsent() {
        MediaAsset asset = asset(1L, MediaAssetStatus.UNIDENTIFIED, null);
        when(this.assetRepository.findAllByLibraryId(7L)).thenReturn(Flux.just(asset));

        StepVerifier.create(this.queries.findByLibrary(7L, null))
                .expectNext(asset)
                .verifyComplete();

        verify(this.assetRepository, never())
                .findAllByLibraryIdAndStatus(7L, MediaAssetStatus.UNIDENTIFIED);
    }

    @Test
    void filtersAssetsByStatus() {
        MediaAsset asset = asset(1L, MediaAssetStatus.UNIDENTIFIED, null);
        when(this.assetRepository.findAllByLibraryIdAndStatus(
                        7L, MediaAssetStatus.UNIDENTIFIED))
                .thenReturn(Flux.just(asset));

        StepVerifier.create(
                        this.queries.findByLibrary(7L, MediaAssetStatus.UNIDENTIFIED))
                .expectNext(asset)
                .verifyComplete();

        verify(this.assetRepository, never()).findAllByLibraryId(7L);
    }

    @Test
    void reportsMissingAssetById() {
        when(this.assetRepository.findById(MediaAssetId.of(1L))).thenReturn(Mono.empty());

        StepVerifier.create(this.queries.findById(MediaAssetId.of(1L)))
                .expectError(MediaAssetNotFoundException.class)
                .verify();
    }

    @Test
    void visibleMovieReturnsItsAsset() {
        MediaAsset asset = asset(1L, MediaAssetStatus.IDENTIFIED, MovieId.of(10L));
        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("Maria", "m@m.com")));
        when(this.catalogItemAccess.requireVisible(MovieId.of(10L), "Maria"))
                .thenReturn(Mono.empty());
        when(this.assetRepository.findByMovieId(MovieId.of(10L))).thenReturn(Mono.just(asset));

        StepVerifier.create(this.queries.findByMovie(MovieId.of(10L)))
                .expectNext(asset)
                .verifyComplete();
    }

    @Test
    void invisibleMovieDoesNotExposeItsAsset() {
        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("Maria", "m@m.com")));
        when(this.catalogItemAccess.requireVisible(MovieId.of(10L), "Maria"))
                .thenReturn(Mono.error(new MovieAccessDeniedException(
                        "Movie not accessible: 10")));

        StepVerifier.create(this.queries.findByMovie(MovieId.of(10L)))
                .expectError(MovieAccessDeniedException.class)
                .verify();

        verify(this.assetRepository, never()).findByMovieId(MovieId.of(10L));
    }

    @Test
    void reportsWhenVisibleMovieHasNoAsset() {
        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("Maria", "m@m.com")));
        when(this.catalogItemAccess.requireVisible(MovieId.of(10L), "Maria"))
                .thenReturn(Mono.empty());
        when(this.assetRepository.findByMovieId(MovieId.of(10L))).thenReturn(Mono.empty());

        StepVerifier.create(this.queries.findByMovie(MovieId.of(10L)))
                .expectError(MediaAssetNotFoundException.class)
                .verify();
    }

    private static MediaAsset asset(
            long id, MediaAssetStatus status, MovieId movieId) {
        Instant now = Instant.now();
        return new MediaAsset(
                MediaAssetId.of(id),
                7L,
                "Dune.mp4",
                1024L,
                "video/mp4",
                status,
                movieId,
                true,
                now,
                now);
    }

}
