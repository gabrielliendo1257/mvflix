package com.gcorp.service.app.mvflix_movies.catalog.application;

import com.gcorp.service.app.mvflix_movies.shared.application.security.UserProvider;
import com.gcorp.service.app.mvflix_movies.catalog.application.port.ManagedObjectDeletionUnavailableException;
import com.gcorp.service.app.mvflix_movies.catalog.domain.media.MediaRepository;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.Movie;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieId;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieRepository;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

/**
 * Entrada única al borrado del catálogo. No es transaccional porque el camino
 * MANAGED contiene una llamada HTTP; las escrituras locales viven en
 * {@link MovieDeletionTransaction}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeleteMovieUseCase {

    private final MovieRepository movieRepository;
    private final MediaRepository mediaRepository;
    private final UserProvider userProvider;
    private final MovieDeletionTransaction deletionTransaction;
    private final ManagedMediaDeletionCoordinator deletionCoordinator;
    @Value("${mvflix.messaging.kafka.enabled:false}")
    private boolean kafkaEnabled;

    public Mono<DeletionOutcome> execute(MovieId id) {
        return this.userProvider.getAuthenticatedUser()
                .flatMap(user -> this.movieRepository.findById(id)
                        // Missing and foreign are deliberately indistinguishable.
                        .filter(movie -> movie.isOwnedBy(user.subject()) || user.isAdmin())
                        .flatMap(movie -> this.deleteOwnedMovie(id, movie))
                        .defaultIfEmpty(new DeletionOutcome.Completed()));
    }

    private Mono<DeletionOutcome> deleteOwnedMovie(MovieId id, Movie movie) {
        if (movie.getStatus() == MovieStatus.DELETING) {
            return this.kafkaEnabled
                    ? Mono.just(new DeletionOutcome.Pending())
                    : this.coordinate(id);
        }

        return this.mediaRepository.findByMovieId(id)
                .flatMap(media -> this.beginManagedDeletion(id))
                // No media row means DRAFT/NONE or LOCAL. LibraryAssetLinks only
                // unlinks the catalog association and never deletes the file.
                .switchIfEmpty(Mono.defer(() -> this.deleteImmediately(id)));
    }

    private Mono<DeletionOutcome> beginManagedDeletion(MovieId id) {
        return (this.kafkaEnabled
                        ? this.deletionTransaction.requestDeletion(id)
                        : this.deletionTransaction.requestDeletionWithoutOutbox(id))
                // Empty CAS means another instance won, or the movie was
                // already removed; process() is safe in either case.
                .then(this.kafkaEnabled
                        ? Mono.just(new DeletionOutcome.Pending())
                        : this.coordinate(id));
    }

    private Mono<DeletionOutcome> coordinate(MovieId id) {
        return this.deletionCoordinator.process(id)
                .<DeletionOutcome>thenReturn(new DeletionOutcome.Completed())
                .onErrorResume(ManagedObjectDeletionUnavailableException.class,
                        error -> {
                            log.info("Borrado pendiente por Storage no disponible: id={}", id.value());
                            return Mono.just(new DeletionOutcome.Pending());
                        });
    }

    private Mono<DeletionOutcome> deleteImmediately(MovieId id) {
        return this.deletionTransaction.deleteImmediately(id)
                .<DeletionOutcome>thenReturn(new DeletionOutcome.Completed());
    }
}
