package com.gcorp.service.app.mvflix_movies.catalog.application;

import com.gcorp.service.app.mvflix_movies.shared.application.security.UserProvider;
import com.gcorp.service.app.mvflix_movies.catalog.domain.media.MediaRepository;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItem;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemId;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemRepository;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemStatus;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

/**
 * Entrada única al borrado del catálogo. La eliminación MANAGED se solicita de
 * forma durable mediante la outbox; las
 * escrituras locales viven en {@link CatalogItemDeletionTransaction}.
 */
@Service
@RequiredArgsConstructor
public class DeleteCatalogItemUseCase {

    private final CatalogItemRepository movieRepository;
    private final MediaRepository mediaRepository;
    private final UserProvider userProvider;
    private final CatalogItemDeletionTransaction deletionTransaction;

    public Mono<DeletionOutcome> execute(CatalogItemId id) {
        return this.userProvider.getAuthenticatedUser()
                .flatMap(user -> this.movieRepository.findById(id)
                        // Missing and foreign are deliberately indistinguishable.
                        .filter(movie -> movie.isOwnedBy(user.subject()) || user.isAdmin())
                        .flatMap(movie -> this.deleteOwnedMovie(id, movie))
                        .defaultIfEmpty(new DeletionOutcome.Completed()));
    }

    private Mono<DeletionOutcome> deleteOwnedMovie(CatalogItemId id, CatalogItem movie) {
        if (movie.getStatus() == CatalogItemStatus.DELETING) {
            return this.deletionTransaction.ensureDeletionRequested(id)
                    .thenReturn(new DeletionOutcome.Pending());
        }

        return this.mediaRepository.findByCatalogItemId(id)
                .flatMap(media -> this.beginManagedDeletion(id))
                // No media row means DRAFT/NONE or LOCAL. LibraryAssetLinks only
                // unlinks the catalog association and never deletes the file.
                .switchIfEmpty(Mono.defer(() -> this.deleteImmediately(id)));
    }

    private Mono<DeletionOutcome> beginManagedDeletion(CatalogItemId id) {
        return this.deletionTransaction.requestDeletion(id)
                .thenReturn(new DeletionOutcome.Pending());
    }

    private Mono<DeletionOutcome> deleteImmediately(CatalogItemId id) {
        return this.deletionTransaction.deleteImmediately(id)
                .<DeletionOutcome>thenReturn(new DeletionOutcome.Completed());
    }
}
