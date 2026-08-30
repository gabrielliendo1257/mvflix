package com.gcorp.service.app.mvflix_movies.catalog.application;

import com.gcorp.service.app.mvflix_movies.catalog.application.port.LibraryAssetLinks;
import com.gcorp.service.app.mvflix_movies.catalog.application.port.ManagedDeletionOutbox;
import com.gcorp.service.app.mvflix_movies.catalog.application.port.ManagedMediaDeletionRequested;
import com.gcorp.service.app.mvflix_movies.catalog.domain.media.MediaRepository;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItem;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemId;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Colaborador transaccional del borrado durable de media MANAGED.
 *
 * <p>{@code requestDeletion}: CAS READY → DELETING (una media READY con objeto
 * en storage entra en borrado). {@code finalizeDeletion}: desvincula los assets
 * de biblioteca LOCAL, borra la película (solo si DELETING) y la cascada
 * elimina el {@code media} administrado y los {@code movie_shares}.
 *
 * <p>NO envuelve llamadas HTTP a storage: el borrado del objeto (M2M
 * {@code movies-catalog}) es un paso SEPARADO del orquestador, antes de la
 * finalización. Aquí solo hay escrituras locales atómicas.
 */
@Service
@RequiredArgsConstructor
public class CatalogItemDeletionTransaction {

    private final CatalogItemRepository movieRepository;
    private final LibraryAssetLinks libraryAssetLinks;
    private final MediaRepository mediaRepository;
    private final ManagedDeletionOutbox managedDeletionOutbox;

    /** CAS READY → DELETING; vacío si la media no estaba READY. */
    @Transactional(transactionManager = "connectionFactoryTransactionManager")
    public Mono<CatalogItem> requestDeletion(CatalogItemId id) {
        return this.requestDeletionDurably(id);
    }

    private Mono<CatalogItem> requestDeletionDurably(CatalogItemId id) {
        return this.movieRepository.markDeleting(id)
                .flatMap(movie -> this.mediaRepository.findByCatalogItemId(id)
                        .switchIfEmpty(Mono.error(new IllegalStateException(
                                "Managed deletion requires media for movie=" + id.value())))
                        .flatMap(media -> this.managedDeletionOutbox
                                .append(ManagedMediaDeletionRequested.create(
                                         id.value(), media.getStorageObjectId().value(), movie.getOwnerUsername(),
                                        media.getObjectKey()))
                                .thenReturn(movie)));
    }

    /** Desvincula assets LOCALES y borra la media DELETING (cascada media/shares). */
    @Transactional(transactionManager = "connectionFactoryTransactionManager")
    public Mono<Void> finalizeDeletion(CatalogItemId id) {
        return this.libraryAssetLinks.unlinkByCatalogItemId(id)
                .then(this.movieRepository.deleteIfDeleting(id))
                .then();
    }

    /** Finaliza una solicitud de Storage; repetirla es un no-op si la fila ya no existe. */
    @Transactional(transactionManager = "connectionFactoryTransactionManager")
    public Mono<Void> finalizeManagedDeletion(CatalogItemId movieId, long storageId) {
        return this.movieRepository.findById(movieId)
                .flatMap(movie -> {
                    if (!movie.isDeleting()) {
                        return Mono.error(new IllegalStateException(
                                "Cannot finalize movie=" + movieId.value()
                                        + ": status=" + movie.getStatus()));
                    }
                     return this.mediaRepository.findByCatalogItemId(movieId)
                         .switchIfEmpty(Mono.error(new IllegalStateException(
                                 "Managed media missing for movie=" + movieId.value())))
                         .flatMap(media -> {
                            if (!Objects.equals(media.getStorageObjectId().value(), storageId)) {
                             return Mono.error(new IllegalStateException(
                                     "Cannot finalize movie=" + movieId.value()
                                             + ": storageId mismatch"));
                           }
                           return this.libraryAssetLinks.unlinkByCatalogItemId(movieId)
                             .then(this.movieRepository.deleteIfDeletingAndStorageId(movieId, storageId))
                             .flatMap(deleted -> deleted
                                     ? Mono.empty()
                                     : Mono.error(new IllegalStateException(
                                             "CatalogItem changed while finalizing movie=" + movieId.value())));
                         });
                 })
                .then();
    }

    /** Borrado atómico de DRAFT/NONE/LOCAL; no elimina archivos de Library. */
    @Transactional(transactionManager = "connectionFactoryTransactionManager")
    public Mono<Void> deleteImmediately(CatalogItemId id) {
        return this.libraryAssetLinks.unlinkByCatalogItemId(id)
                .then(this.movieRepository.deleteById(id))
                .then();
    }

    /**
     * Garantiza que una película DELETING tenga una solicitud Kafka persistida.
     * Es el puente de migración para películas iniciadas antes de habilitar Kafka.
     */
    @Transactional(transactionManager = "connectionFactoryTransactionManager")
    public Mono<Void> ensureDeletionRequested(CatalogItemId id) {
        return this.movieRepository.findById(id)
                .flatMap(movie -> {
                    if (!movie.isDeleting()) {
                        return Mono.error(new IllegalStateException(
                                "Managed deletion requires DELETING movie=" + id.value()));
                    }
                    return this.mediaRepository.findByCatalogItemId(id)
                            .switchIfEmpty(Mono.error(new IllegalStateException(
                                    "Managed deletion requires media for movie=" + id.value())))
                            .flatMap(media -> this.managedDeletionOutbox.append(
                                    ManagedMediaDeletionRequested.create(
                                             id.value(), media.getStorageObjectId().value(), movie.getOwnerUsername(),
                                            media.getObjectKey())));
                })
                .then();
    }
}
