package com.gcorp.service.app.mvflix_movies.catalog.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gcorp.service.app.mvflix_movies.shared.application.security.AuthenticatedUser;
import com.gcorp.service.app.mvflix_movies.shared.application.security.UserProvider;
import com.gcorp.service.app.mvflix_movies.catalog.domain.asset.MediaRepository;
import com.gcorp.service.app.mvflix_movies.catalog.domain.asset.ManagedMediaAsset;
import com.gcorp.service.app.mvflix_movies.catalog.domain.asset.MediaId;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.EnrichmentStatus;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemKind;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItem;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemId;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieMetadata;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemNotFoundException;
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

import java.time.Instant;

@ExtendWith(MockitoExtension.class)
class DeleteCatalogItemUseCaseTest {

    @Mock private CatalogItemRepository movieRepository;
    @Mock private MediaRepository mediaRepository;
    @Mock private UserProvider userProvider;
    @Mock private CatalogItemDeletionTransaction deletionTransaction;

    @InjectMocks private DeleteCatalogItemUseCase useCase;

    private static CatalogItem movie(long id, String owner) {
        return new CatalogItem(
                CatalogItemId.of(id), owner, "Dune", CatalogItemStatus.READY, EnrichmentStatus.ENRICHED,
                77L, null, CatalogItemVisibility.PRIVATE, java.util.Set.of(), CatalogItemKind.MOVIE);
    }

    @Test
    void ownerDeletesOwnMovie() {
        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("Javier", "j@m.com")));
        when(this.movieRepository.findById(CatalogItemId.of(1L))).thenReturn(Mono.just(movie(1L, "Javier")));
        when(this.mediaRepository.findByCatalogItemId(CatalogItemId.of(1L))).thenReturn(Mono.empty());
        when(this.deletionTransaction.deleteImmediately(CatalogItemId.of(1L))).thenReturn(Mono.empty());

        StepVerifier.create(this.useCase.execute(CatalogItemId.of(1L)))
                .expectNext(new DeletionOutcome.Completed())
                .verifyComplete();

        verify(this.deletionTransaction).deleteImmediately(CatalogItemId.of(1L));
    }

    @Test
    void adminCanDeleteMoviesOfOthers() {
        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("Admin", "a@m.com",
                        java.util.Set.of(AuthenticatedUser.ADMIN_ROLE))));
        when(this.movieRepository.findById(CatalogItemId.of(1L))).thenReturn(Mono.just(movie(1L, "Javier")));
        when(this.mediaRepository.findByCatalogItemId(CatalogItemId.of(1L))).thenReturn(Mono.empty());
        when(this.deletionTransaction.deleteImmediately(CatalogItemId.of(1L))).thenReturn(Mono.empty());

        StepVerifier.create(this.useCase.execute(CatalogItemId.of(1L)))
                .expectNext(new DeletionOutcome.Completed())
                .verifyComplete();
    }

    @Test
    void nonOwnerWithoutAdminRoleGetsNotFoundWithoutRevealingExistence() {
        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("Maria", "m@m.com")));
        when(this.movieRepository.findById(CatalogItemId.of(1L))).thenReturn(Mono.just(movie(1L, "Javier")));

        StepVerifier.create(this.useCase.execute(CatalogItemId.of(1L)))
                .expectNext(new DeletionOutcome.Completed())
                .verifyComplete();

        verify(this.deletionTransaction, never()).deleteImmediately(any());
    }

    @Test
    void managedMovieIsMarkedDeletingAndRequestedDurably() {
        CatalogItem movie = movie(1L, "Javier");
        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("Javier", "j@m.com")));
        when(this.movieRepository.findById(CatalogItemId.of(1L))).thenReturn(Mono.just(movie));
        when(this.mediaRepository.findByCatalogItemId(CatalogItemId.of(1L)))
                .thenReturn(Mono.just(new ManagedMediaAsset(MediaId.of(9L), CatalogItemId.of(1L), 77L, "k", Instant.now())));
        when(this.deletionTransaction.requestDeletion(CatalogItemId.of(1L)))
                .thenReturn(Mono.just(movie));

        StepVerifier.create(this.useCase.execute(CatalogItemId.of(1L)))
                .expectNext(new DeletionOutcome.Pending())
                .verifyComplete();

        verify(this.deletionTransaction).requestDeletion(CatalogItemId.of(1L));
    }

    @Test
    void durableDeletionRequestFailureIsPropagated() {
        CatalogItem movie = movie(1L, "Javier");
        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("Javier", "j@m.com")));
        when(this.movieRepository.findById(CatalogItemId.of(1L))).thenReturn(Mono.just(movie));
        when(this.mediaRepository.findByCatalogItemId(CatalogItemId.of(1L)))
                .thenReturn(Mono.just(new ManagedMediaAsset(MediaId.of(9L), CatalogItemId.of(1L), 77L, "k", Instant.now())));
        RuntimeException failure = new RuntimeException("outbox unavailable");
        when(this.deletionTransaction.requestDeletion(CatalogItemId.of(1L)))
                .thenReturn(Mono.error(failure));

        StepVerifier.create(this.useCase.execute(CatalogItemId.of(1L)))
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void managedDeletionAlwaysUsesOutbox() {
        CatalogItem movie = movie(1L, "Javier");
        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("Javier", "j@m.com")));
        when(this.movieRepository.findById(CatalogItemId.of(1L))).thenReturn(Mono.just(movie));
        when(this.mediaRepository.findByCatalogItemId(CatalogItemId.of(1L)))
                .thenReturn(Mono.just(new ManagedMediaAsset(MediaId.of(9L), CatalogItemId.of(1L), 77L, "k", Instant.now())));
        when(this.deletionTransaction.requestDeletion(CatalogItemId.of(1L))).thenReturn(Mono.just(movie));

        StepVerifier.create(this.useCase.execute(CatalogItemId.of(1L)))
                .expectNext(new DeletionOutcome.Pending())
                .verifyComplete();

        verify(this.deletionTransaction).requestDeletion(CatalogItemId.of(1L));
    }

    @Test
    void alreadyDeletingMovieEnsuresDurableRequest() {
        CatalogItem movie = new CatalogItem(
                CatalogItemId.of(1L), "Javier", "Dune", CatalogItemStatus.DELETING, EnrichmentStatus.ENRICHED,
                77L, null, CatalogItemVisibility.PRIVATE, java.util.Set.of(), CatalogItemKind.MOVIE);
        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("Javier", "j@m.com")));
        when(this.movieRepository.findById(CatalogItemId.of(1L))).thenReturn(Mono.just(movie));
        when(this.deletionTransaction.ensureDeletionRequested(CatalogItemId.of(1L)))
                .thenReturn(Mono.empty());

        StepVerifier.create(this.useCase.execute(CatalogItemId.of(1L)))
                .expectNext(new DeletionOutcome.Pending())
                .verifyComplete();

        verify(this.deletionTransaction).ensureDeletionRequested(CatalogItemId.of(1L));
    }
}
