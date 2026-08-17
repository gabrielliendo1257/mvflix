package com.gcorp.service.app.mvflix_movies.application.movie;

import com.gcorp.service.app.mvflix_movies.app.security.UserProvider;
import com.gcorp.service.app.mvflix_movies.domain.movie.Movie;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieConflictException;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieNotFoundException;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieRepository;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompleteMovieUseCase {

    private final MovieRepository movieRepository;
    private final UserProvider userProvider;

    public Mono<Movie> execute(Long id, Long objectId, String objectKey) {
        return this.userProvider
                .getAuthenticatedUser()
                .flatMap(user -> this.movieRepository
                        .completeIfDraft(id, user.subject(), objectId, objectKey)
                        .doOnNext(movie -> log.info(
                                "Pelicula completada: id={} owner={} object_id={} object_key={}",
                                id, user.subject(), objectId, objectKey))
                        .switchIfEmpty(
                                this.resolveConflict(id, user.subject(), objectId, objectKey)));
    }

    /**
     * Reconciliación cuando el CAS no transicionó: distingue 404 (no existe / no es del dueño),
     * no-op idempotente (ya READY con el mismo object_key) y 409 (estado no completable).
     */
    private Mono<Movie> resolveConflict(Long id, String ownerUsername, Long objectId,
            String objectKey) {
        return this.movieRepository
                .findById(id)
                .flatMap(movie -> {
                    if (!movie.getOwnerUsername().equals(ownerUsername)) {
                        return Mono.error(
                                new MovieNotFoundException("Movie not found: " + id));
                    }
                    if (movie.getStatus() == MovieStatus.READY
                            && objectKey.equals(movie.getObjectKey())) {
                        log.info("Pelicula {} ya READY con el mismo object_key: no-op", id);
                        return Mono.just(movie);
                    }
                    log.warn(
                            "Pelicula {} no completable: status={} object_key_actual={} pedido={}",
                            id, movie.getStatus(), movie.getObjectKey(), objectKey);
                    return Mono.error(new MovieConflictException(
                            "Movie is not in DRAFT state: " + id));
                })
                .switchIfEmpty(
                        Mono.error(new MovieNotFoundException("Movie not found: " + id)));
    }
}