package com.gcorp.service.app.mvflix_movies.catalog.application;

import com.gcorp.service.app.mvflix_movies.catalog.application.port.ManagedObjectDeletion;
import com.gcorp.service.app.mvflix_movies.catalog.application.port.ManagedObjectReference;
import com.gcorp.service.app.mvflix_movies.catalog.domain.media.Media;
import com.gcorp.service.app.mvflix_movies.catalog.domain.media.MediaRepository;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.Movie;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieId;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

/**
 * Coordina el borrado durable sin extender una transacción local sobre Storage.
 *
 * <p>La llamada remota ocurre antes de {@link MovieDeletionTransaction}, que
 * solo contiene las escrituras locales atómicas. Si Storage falla, la cadena
 * termina y la película permanece en {@code DELETING}; una ejecución posterior
 * puede reanudarla.
 */
@Service
@RequiredArgsConstructor
public class ManagedMediaDeletionCoordinator {

    private final MediaRepository mediaRepository;
    private final ManagedObjectDeletion storageDeletion;
    private final MovieDeletionTransaction deletionTransaction;
    private final MovieRepository movieRepository;

    public Mono<Void> process(MovieId movieId) {
        return this.movieRepository.findById(movieId)
                .flatMap(movie -> this.mediaRepository.findByMovieId(movieId)
                        .flatMap(media -> this.storageDeletion
                                .delete(referenceOf(movie, media))
                                .thenReturn(true))
                        // Sin media MANAGED solo queda limpiar el catálogo y
                        // desvincular LOCAL; nunca se borra el archivo local.
                        .defaultIfEmpty(false)
                        .flatMap(ignored -> this.deletionTransaction.finalizeDeletion(movieId)));
    }

    private ManagedObjectReference referenceOf(Movie movie, Media media) {
        return new ManagedObjectReference(
                media.getObjectId(), movie.getOwnerUsername(), media.getObjectKey());
    }
}
