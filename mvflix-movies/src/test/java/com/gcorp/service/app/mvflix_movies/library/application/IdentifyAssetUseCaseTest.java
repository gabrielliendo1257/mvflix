package com.gcorp.service.app.mvflix_movies.library.application;

import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemKind;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gcorp.service.app.mvflix_movies.shared.application.security.AuthenticatedUser;
import com.gcorp.service.app.mvflix_movies.shared.application.security.UserProvider;
import com.gcorp.service.app.mvflix_movies.library.application.port.CatalogItemEnricher;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAsset;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAssetAlreadyIdentifiedException;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAssetId;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAssetNotFoundException;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAssetRepository;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAssetStatus;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemId;

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
    @Mock private CatalogItemEnricher catalogItemEnricher;
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
                        Instant.now(),
                        "Javier");
        when(this.assetRepository.findById(MediaAssetId.of(1L))).thenReturn(Mono.just(asset));
        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("Javier", "j@m.com")));
        when(this.identifyAssetTransaction.execute(
                        eq(asset), eq("Javier"), eq("Dune"), eq(CatalogItemKind.MOVIE)))
                .thenReturn(Mono.just(new IdentificationResult(
                        asset.identify(CatalogItemId.of(50L)), CatalogItemId.of(50L))));

        StepVerifier.create(this.useCase.execute(MediaAssetId.of(1L), "Dune", null, null))
                .expectNextMatches(identified ->
                        identified.isIdentified()
                                && identified.getCatalogItemId().equals(CatalogItemId.of(50L)))
                .verifyComplete();

        verify(this.identifyAssetTransaction).execute(
                asset, "Javier", "Dune", CatalogItemKind.MOVIE);
        verify(this.catalogItemEnricher, never()).enrich(any(CatalogItemId.class), any(Long.class));
    }

    @Test
    void nonOwnerCannotIdentifySomeoneElsesAsset() {
        MediaAsset foreign =
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
                        Instant.now(),
                        "admin");

        when(this.assetRepository.findById(MediaAssetId.of(1L))).thenReturn(Mono.just(foreign));
        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("Maria", "m@m.com")));

        // Sin revelar existencia: mismo error que un id inexistente.
        StepVerifier.create(this.useCase.execute(MediaAssetId.of(1L), "Dune", null, null))
                .expectError(MediaAssetNotFoundException.class)
                .verify();

        verify(this.identifyAssetTransaction, never())
                .execute(any(MediaAsset.class), any(String.class), any(String.class), any());
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
                        CatalogItemId.of(50L),
                        true,
                        Instant.now(),
                        Instant.now(),
                        "Javier");

        when(this.assetRepository.findById(MediaAssetId.of(1L))).thenReturn(Mono.just(identified));
        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("Javier", "j@m.com")));

        StepVerifier.create(this.useCase.execute(MediaAssetId.of(1L), "Dune", null, null))
                .expectNextMatches(asset -> asset.getCatalogItemId().equals(CatalogItemId.of(50L)))
                .verifyComplete();

        verify(this.identifyAssetTransaction, never())
                .execute(
                        any(MediaAsset.class),
                        any(String.class),
                        any(String.class),
                        any(CatalogItemKind.class));
        verify(this.catalogItemEnricher, never()).enrich(any(CatalogItemId.class), any(Long.class));
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
                        Instant.now(),
                        "Javier");
        MediaAsset winner = asset.identify(CatalogItemId.of(50L));

        when(this.assetRepository.findById(MediaAssetId.of(1L)))
                .thenReturn(Mono.just(asset), Mono.just(winner));
        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("Javier", "j@m.com")));
        when(this.identifyAssetTransaction.execute(
                        eq(asset), eq("Javier"), eq("Dune"), eq(CatalogItemKind.MOVIE)))
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
                        Instant.now(),
                        "Javier");
        when(this.assetRepository.findById(MediaAssetId.of(1L))).thenReturn(Mono.just(asset));
        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("Javier", "j@m.com")));
        when(this.identifyAssetTransaction.execute(
                        eq(asset),
                        eq("Javier"),
                        eq("Interstellar (2014)"),
                        eq(CatalogItemKind.MOVIE)))
                .thenReturn(Mono.just(new IdentificationResult(
                        asset.identify(CatalogItemId.of(50L)), CatalogItemId.of(50L))));
        when(this.catalogItemEnricher.enrich(CatalogItemId.of(50L), 157336L))
                .thenReturn(Mono.empty());

        StepVerifier.create(this.useCase.execute(MediaAssetId.of(1L), "Interstellar (2014)", 157336L, null))
                .expectNextMatches(identified ->
                        identified.isIdentified()
                                && identified.getCatalogItemId().equals(CatalogItemId.of(50L)))
                .verifyComplete();

        verify(this.catalogItemEnricher).enrich(CatalogItemId.of(50L), 157336L);
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
                        Instant.now(),
                        "Javier");
        when(this.assetRepository.findById(MediaAssetId.of(1L))).thenReturn(Mono.just(asset));
        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("Javier", "j@m.com")));
        when(this.identifyAssetTransaction.execute(
                        eq(asset), eq("Javier"), eq("Dune"), eq(CatalogItemKind.MOVIE)))
                .thenReturn(Mono.just(new IdentificationResult(
                        asset.identify(CatalogItemId.of(50L)), CatalogItemId.of(50L))));
        when(this.catalogItemEnricher.enrich(CatalogItemId.of(50L), 123L))
                .thenReturn(Mono.error(new IllegalStateException("TMDB down")));

        StepVerifier.create(this.useCase.execute(MediaAssetId.of(1L), "Dune", 123L, null))
                .expectNextMatches(identified -> identified.isIdentified())
                .verifyComplete();

        verify(this.identifyAssetTransaction).execute(
                asset, "Javier", "Dune", CatalogItemKind.MOVIE);
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
                        Instant.now(),
                        "Javier");
        when(this.assetRepository.findById(MediaAssetId.of(1L))).thenReturn(Mono.just(asset));
        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("Javier", "j@m.com")));
        when(this.identifyAssetTransaction.execute(
                        eq(asset), eq("Javier"), eq("Dune"), eq(CatalogItemKind.MOVIE)))
                .thenReturn(Mono.just(new IdentificationResult(
                        asset.identify(CatalogItemId.of(50L)), CatalogItemId.of(50L))));
        when(this.catalogItemEnricher.enrich(CatalogItemId.of(50L), 123L))
                .thenReturn(Mono.never());

        StepVerifier.withVirtualTime(() ->
                        this.useCase.execute(MediaAssetId.of(1L), "Dune", 123L, null))
                .thenAwait(java.time.Duration.ofSeconds(21))
                .expectNextMatches(identified ->
                        identified.isIdentified()
                                && identified.getCatalogItemId().equals(CatalogItemId.of(50L)))
                .verifyComplete();

        verify(this.identifyAssetTransaction).execute(
                asset, "Javier", "Dune", CatalogItemKind.MOVIE);
    }
}
