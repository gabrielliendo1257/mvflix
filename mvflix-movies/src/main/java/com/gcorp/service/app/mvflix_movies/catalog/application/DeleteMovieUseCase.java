package com.gcorp.service.app.mvflix_movies.catalog.application;

import com.gcorp.service.app.mvflix_movies.shared.application.security.UserProvider;
import com.gcorp.service.app.mvflix_movies.catalog.domain.media.MediaRepository;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.Movie;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieId;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieRepository;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieStatus;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

/**
 * Entrada única al borrado del catálogo. La eliminación MANAGED se solicita de
 * forma durable mediante la outbox; las
 * escrituras locales viven en {@link MovieDeletionTransaction}.
 */
@Service
@RequiredArgsConstructor
public class DeleteMovieUseCase {

    private final MovieRepository movieRepository;
    private final MediaRepository mediaRepository;
    private final UserProvider userProvider;
    private final MovieDeletionTransaction deletionTransaction;

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
            return this.deletionTransaction.ensureDeletionRequested(id)
                    .thenReturn(new DeletionOutcome.Pending());
        }

        return this.mediaRepository.findByMovieId(id)
                .flatMap(media -> this.beginManagedDeletion(id))
                // No media row means DRAFT/NONE or LOCAL. LibraryAssetLinks only
                // unlinks the catalog association and never deletes the file.
                .switchIfEmpty(Mono.defer(() -> this.deleteImmediately(id)));
    }

    private Mono<DeletionOutcome> beginManagedDeletion(MovieId id) {
        return this.deletionTransaction.requestDeletion(id)
                .thenReturn(new DeletionOutcome.Pending());
    }

    private Mono<DeletionOutcome> deleteImmediately(MovieId id) {
        return this.deletionTransaction.deleteImmediately(id)
                .<DeletionOutcome>thenReturn(new DeletionOutcome.Completed());
    }
}
