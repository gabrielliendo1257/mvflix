package com.gcorp.service.app.mvflix_movies.library.application;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gcorp.service.app.mvflix_movies.shared.application.security.AuthenticatedUser;
import com.gcorp.service.app.mvflix_movies.shared.application.security.UserProvider;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAsset;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAssetId;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAssetNotFoundException;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAssetRepository;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAssetStatus;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItemAccessDeniedException;
import com.gcorp.service.app.mvflix_movies.library.domain.CatalogItemId;
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
import java.util.Set;

@ExtendWith(MockitoExtension.class)
class MediaAssetQueriesTest {

    private static final long LIBRARY_ID = 7L;

    @Mock private MediaAssetRepository assetRepository;
    @Mock private CatalogItemAccess catalogItemAccess;
    @Mock private UserProvider userProvider;

    @InjectMocks private MediaAssetQueries queries;

    private void requester(String subject, boolean admin) {
        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser(
                        subject, subject + "@m.com",
                        admin ? Set.of(AuthenticatedUser.ADMIN_ROLE) : Set.of())));
    }

    @Test
    void listsAllAssetsWhenStatusIsAbsent() {
        this.requester("pepe", false);
        MediaAsset asset = asset(1L, MediaAssetStatus.UNIDENTIFIED, null, "pepe");
        when(this.assetRepository.findAllByLibraryId(LIBRARY_ID)).thenReturn(Flux.just(asset));

        StepVerifier.create(this.queries.findByLibrary(LIBRARY_ID, null))
                .expectNext(asset)
                .verifyComplete();

        verify(this.assetRepository, never())
                .findAllByLibraryIdAndStatus(LIBRARY_ID, MediaAssetStatus.UNIDENTIFIED);
    }

    @Test
    void filtersAssetsByStatus() {
        this.requester("pepe", false);
        MediaAsset asset = asset(1L, MediaAssetStatus.UNIDENTIFIED, null, "pepe");
        when(this.assetRepository.findAllByLibraryIdAndStatus(
                        LIBRARY_ID, MediaAssetStatus.UNIDENTIFIED))
                .thenReturn(Flux.just(asset));

        StepVerifier.create(
                        this.queries.findByLibrary(LIBRARY_ID, MediaAssetStatus.UNIDENTIFIED))
                .expectNext(asset)
                .verifyComplete();

        verify(this.assetRepository, never()).findAllByLibraryId(LIBRARY_ID);
    }

    @Test
    void nonAdminOnlySeesOwnDiscoveriesInTheLibrary() {
        this.requester("pepe", false);
        MediaAsset mine = asset(1L, MediaAssetStatus.UNIDENTIFIED, null, "pepe");
        MediaAsset theirs = asset(2L, MediaAssetStatus.IDENTIFIED,
                CatalogItemId.of(10L), "admin");
        MediaAsset orphan = asset(3L, MediaAssetStatus.UNIDENTIFIED, null, null);
        when(this.assetRepository.findAllByLibraryId(LIBRARY_ID))
                .thenReturn(Flux.just(mine, theirs, orphan));

        StepVerifier.create(this.queries.findByLibrary(LIBRARY_ID, null))
                .expectNext(mine)
                .verifyComplete();
    }

    @Test
    void adminSeesEveryDiscoveryIncludingOrphans() {
        this.requester("admin", true);
        MediaAsset mine = asset(1L, MediaAssetStatus.UNIDENTIFIED, null, "pepe");
        MediaAsset orphan = asset(3L, MediaAssetStatus.UNIDENTIFIED, null, null);
        when(this.assetRepository.findAllByLibraryId(LIBRARY_ID))
                .thenReturn(Flux.just(mine, orphan));

        StepVerifier.create(this.queries.findByLibrary(LIBRARY_ID, null))
                .expectNext(mine, orphan)
                .verifyComplete();
    }

    @Test
    void reportsMissingAssetById() {
        this.requester("pepe", false);
        when(this.assetRepository.findById(MediaAssetId.of(1L))).thenReturn(Mono.empty());

        StepVerifier.create(this.queries.findById(MediaAssetId.of(1L)))
                .expectError(MediaAssetNotFoundException.class)
                .verify();
    }

    @Test
    void identifiedAssetIsVisibleThroughItsMovie() {
        this.requester("Maria", false);
        MediaAsset asset = asset(1L, MediaAssetStatus.IDENTIFIED,
                CatalogItemId.of(10L), "admin");
        when(this.assetRepository.findById(MediaAssetId.of(1L))).thenReturn(Mono.just(asset));
        when(this.catalogItemAccess.requireVisible(CatalogItemId.of(10L), "Maria"))
                .thenReturn(Mono.empty());

        StepVerifier.create(this.queries.findById(MediaAssetId.of(1L)))
                .expectNext(asset)
                .verifyComplete();
    }

    @Test
    void unidentifiedAssetIsManagementOnlyForNonAdmins() {
        this.requester("pepe", false);
        MediaAsset asset = asset(1L, MediaAssetStatus.UNIDENTIFIED, null, "admin");
        when(this.assetRepository.findById(MediaAssetId.of(1L))).thenReturn(Mono.just(asset));

        StepVerifier.create(this.queries.findById(MediaAssetId.of(1L)))
                .expectError(MediaAssetNotFoundException.class)
                .verify();
    }

    @Test
    void invisibleMovieDoesNotExposeAssetByIdEither() {
        this.requester("Maria", false);
        MediaAsset asset = asset(1L, MediaAssetStatus.IDENTIFIED,
                CatalogItemId.of(10L), "admin");
        when(this.assetRepository.findById(MediaAssetId.of(1L))).thenReturn(Mono.just(asset));
        when(this.catalogItemAccess.requireVisible(CatalogItemId.of(10L), "Maria"))
                .thenReturn(Mono.error(new CatalogItemAccessDeniedException(
                        "Movie not accessible: 10")));

        // Sin revelar existencia: el mismo error que un id inexistente.
        StepVerifier.create(this.queries.findById(MediaAssetId.of(1L)))
                .expectError(MediaAssetNotFoundException.class)
                .verify();

        verify(this.assetRepository, never()).findByCatalogItemId(CatalogItemId.of(10L));
    }

    @Test
    void visibleMovieReturnsItsAsset() {
        this.requester("Maria", false);
        MediaAsset asset = asset(1L, MediaAssetStatus.IDENTIFIED, CatalogItemId.of(10L), "admin");
        when(this.catalogItemAccess.requireVisible(CatalogItemId.of(10L), "Maria"))
                .thenReturn(Mono.empty());
        when(this.assetRepository.findByCatalogItemId(CatalogItemId.of(10L)))
                .thenReturn(Mono.just(asset));

        StepVerifier.create(this.queries.findByCatalogItem(CatalogItemId.of(10L)))
                .expectNext(asset)
                .verifyComplete();
    }

    @Test
    void invisibleMovieDoesNotExposeItsAsset() {
        this.requester("Maria", false);
        when(this.catalogItemAccess.requireVisible(CatalogItemId.of(10L), "Maria"))
                .thenReturn(Mono.error(new CatalogItemAccessDeniedException(
                        "Movie not accessible: 10")));

        StepVerifier.create(this.queries.findByCatalogItem(CatalogItemId.of(10L)))
                .expectError(CatalogItemAccessDeniedException.class)
                .verify();

        verify(this.assetRepository, never()).findByCatalogItemId(CatalogItemId.of(10L));
    }

    @Test
    void reportsWhenVisibleMovieHasNoAsset() {
        this.requester("Maria", false);
        when(this.catalogItemAccess.requireVisible(CatalogItemId.of(10L), "Maria"))
                .thenReturn(Mono.empty());
        when(this.assetRepository.findByCatalogItemId(CatalogItemId.of(10L))).thenReturn(Mono.empty());

        StepVerifier.create(this.queries.findByCatalogItem(CatalogItemId.of(10L)))
                .expectError(MediaAssetNotFoundException.class)
                .verify();
    }

    private static MediaAsset asset(
            long id, MediaAssetStatus status, CatalogItemId catalogItemId, String discoveredBy) {
        Instant now = Instant.now();
        return new MediaAsset(
                MediaAssetId.of(id),
                LIBRARY_ID,
                "Dune.mp4",
                1024L,
                "video/mp4",
                status,
                catalogItemId,
                true,
                now,
                now,
                discoveredBy);
    }
}
