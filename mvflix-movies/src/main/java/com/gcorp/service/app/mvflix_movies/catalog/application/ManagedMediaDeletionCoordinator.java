package com.gcorp.service.app.mvflix_movies.catalog.application;

import com.gcorp.service.app.mvflix_movies.catalog.application.port.ManagedObjectDeletion;
import com.gcorp.service.app.mvflix_movies.catalog.application.port.ManagedObjectReference;
import com.gcorp.service.app.mvflix_movies.catalog.domain.asset.ManagedMediaAsset;
import com.gcorp.service.app.mvflix_movies.catalog.domain.asset.MediaRepository;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItem;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemId;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

/**
 * Coordina el borrado durable sin extender una transacción local sobre Storage.
 *
 * <p>La llamada remota ocurre antes de {@link CatalogItemDeletionTransaction}, que
 * solo contiene las escrituras locales atómicas. Si Storage falla, la cadena
 * termina y la película permanece en {@code DELETING}; una ejecución posterior
 * puede reanudarla.
 */
@Service
@RequiredArgsConstructor
public class ManagedMediaDeletionCoordinator {

    private final MediaRepository mediaRepository;
    private final ManagedObjectDeletion storageDeletion;
    private final CatalogItemDeletionTransaction deletionTransaction;
    private final CatalogItemRepository movieRepository;

    public Mono<Void> process(CatalogItemId movieId) {
        return this.movieRepository.findById(movieId)
                .flatMap(movie -> this.mediaRepository.findByCatalogItemId(movieId)
                        .flatMap(media -> this.storageDeletion
                                .delete(referenceOf(movie, media))
                                .thenReturn(true))
                        // Sin media MANAGED solo queda limpiar el catálogo y
                        // desvincular LOCAL; nunca se borra el archivo local.
                        .defaultIfEmpty(false)
                        .flatMap(ignored -> this.deletionTransaction.finalizeDeletion(movieId)));
    }

    private ManagedObjectReference referenceOf(CatalogItem movie, ManagedMediaAsset media) {
        return new ManagedObjectReference(
                media.getStorageObjectId().value(), movie.getOwnerUsername(), media.getObjectKey());
    }
}
