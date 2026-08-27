package com.gcorp.service.app.mvflix_movies.catalog.application;

import com.gcorp.service.app.mvflix_movies.catalog.application.port.LibraryAssetLinks;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.Movie;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieId;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import reactor.core.publisher.Mono;

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

    /** CAS READY → DELETING; vacío si la media no estaba READY. */
    public Mono<Movie> requestDeletion(MovieId id) {
        return this.movieRepository.markDeleting(id);
    }

    /** Desvincula assets LOCALES y borra la media DELETING (cascada media/shares). */
    @Transactional(transactionManager = "connectionFactoryTransactionManager")
    public Mono<Void> finalizeDeletion(MovieId id) {
        return this.libraryAssetLinks.unlinkByMovieId(id)
                .then(this.movieRepository.deleteIfDeleting(id))
                .then();
    }
}
