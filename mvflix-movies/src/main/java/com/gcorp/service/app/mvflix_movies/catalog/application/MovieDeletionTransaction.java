package com.gcorp.service.app.mvflix_movies.catalog.application;

import com.gcorp.service.app.mvflix_movies.catalog.application.port.LibraryAssetLinks;
import com.gcorp.service.app.mvflix_movies.catalog.application.port.ManagedDeletionOutbox;
import com.gcorp.service.app.mvflix_movies.catalog.application.port.ManagedMediaDeletionRequested;
import com.gcorp.service.app.mvflix_movies.catalog.domain.media.MediaRepository;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.Movie;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieId;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieRepository;

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
public class MovieDeletionTransaction {

    private final MovieRepository movieRepository;
    private final LibraryAssetLinks libraryAssetLinks;
    private final MediaRepository mediaRepository;
    private final ManagedDeletionOutbox managedDeletionOutbox;

    /** CAS READY → DELETING; vacío si la media no estaba READY. */
    @Transactional(transactionManager = "connectionFactoryTransactionManager")
    public Mono<Movie> requestDeletion(MovieId id) {
        return this.requestDeletionDurably(id);
    }

    private Mono<Movie> requestDeletionDurably(MovieId id) {
        return this.movieRepository.markDeleting(id)
                .flatMap(movie -> this.mediaRepository.findByMovieId(id)
                        .switchIfEmpty(Mono.error(new IllegalStateException(
                                "Managed deletion requires media for movie=" + id.value())))
                        .flatMap(media -> this.managedDeletionOutbox
                                .append(ManagedMediaDeletionRequested.create(
                                        id.value(), media.getObjectId(), movie.getOwnerUsername(),
                                        media.getObjectKey()))
                                .thenReturn(movie)));
    }

    /** Desvincula assets LOCALES y borra la media DELETING (cascada media/shares). */
    @Transactional(transactionManager = "connectionFactoryTransactionManager")
    public Mono<Void> finalizeDeletion(MovieId id) {
        return this.libraryAssetLinks.unlinkByMovieId(id)
                .then(this.movieRepository.deleteIfDeleting(id))
                .then();
    }

    /** Finaliza una solicitud de Storage; repetirla es un no-op si la fila ya no existe. */
    @Transactional(transactionManager = "connectionFactoryTransactionManager")
    public Mono<Void> finalizeManagedDeletion(MovieId movieId, long storageId) {
        return this.movieRepository.findById(movieId)
                .flatMap(movie -> {
                    if (!movie.isDeleting()) {
                        return Mono.error(new IllegalStateException(
                                "Cannot finalize movie=" + movieId.value()
                                        + ": status=" + movie.getStatus()));
                    }
                    if (!Objects.equals(movie.getObjectId(), storageId)) {
                        return Mono.error(new IllegalStateException(
                                "Cannot finalize movie=" + movieId.value()
                                        + ": storageId mismatch"));
                    }
                    return this.libraryAssetLinks.unlinkByMovieId(movieId)
                            .then(this.movieRepository.deleteIfDeletingAndStorageId(movieId, storageId))
                            .flatMap(deleted -> deleted
                                    ? Mono.empty()
                                    : Mono.error(new IllegalStateException(
                                            "Movie changed while finalizing movie=" + movieId.value())));
                })
                .then();
    }

    /** Borrado atómico de DRAFT/NONE/LOCAL; no elimina archivos de Library. */
    @Transactional(transactionManager = "connectionFactoryTransactionManager")
    public Mono<Void> deleteImmediately(MovieId id) {
        return this.libraryAssetLinks.unlinkByMovieId(id)
                .then(this.movieRepository.deleteById(id))
                .then();
    }

    /**
     * Garantiza que una película DELETING tenga una solicitud Kafka persistida.
     * Es el puente de migración para películas iniciadas antes de habilitar Kafka.
     */
    @Transactional(transactionManager = "connectionFactoryTransactionManager")
    public Mono<Void> ensureDeletionRequested(MovieId id) {
        return this.movieRepository.findById(id)
                .flatMap(movie -> {
                    if (!movie.isDeleting()) {
                        return Mono.error(new IllegalStateException(
                                "Managed deletion requires DELETING movie=" + id.value()));
                    }
                    return this.mediaRepository.findByMovieId(id)
                            .switchIfEmpty(Mono.error(new IllegalStateException(
                                    "Managed deletion requires media for movie=" + id.value())))
                            .flatMap(media -> this.managedDeletionOutbox.append(
                                    ManagedMediaDeletionRequested.create(
                                            id.value(), media.getObjectId(), movie.getOwnerUsername(),
                                            media.getObjectKey())));
                })
                .then();
    }
}
