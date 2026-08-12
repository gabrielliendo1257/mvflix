package com.gcorp.service.app.mvflix_movies.domain.service;

import com.gcorp.service.app.mvflix_movies.app.security.AuthenticatedUser;
import com.gcorp.service.app.mvflix_movies.app.security.UserProvider;
import com.gcorp.service.app.mvflix_movies.domain.exceptions.MovieConflictException;
import com.gcorp.service.app.mvflix_movies.domain.exceptions.MovieNotFoundException;
import com.gcorp.service.app.mvflix_movies.domain.model.Movie;
import com.gcorp.service.app.mvflix_movies.domain.model.MovieStatus;
import com.gcorp.service.app.mvflix_movies.domain.ports.MovieRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class MovieServiceImpl implements MovieService {

    private final MovieRepository movieRepository;
    private final UserProvider userProvider;

    public MovieServiceImpl(MovieRepository movieRepository, UserProvider userProvider) {
        this.movieRepository = movieRepository;
        this.userProvider = userProvider;
    }

    @Override
    public Mono<Movie> create(CreateMovieCommand command) {
        return this.userProvider
                .getAuthenticatedUser()
                .doOnNext(user -> log.info("Creando pelicula en DRAFT: owner={} title={}",
                        user.subject(), command.metadata().title()))
                .flatMap(user -> {
                    Movie draft = new Movie(
                            null,
                            user.subject(),
                            command.metadata().title(),
                            MovieStatus.DRAFT,
                            null,
                            command.metadata());
                    return this.movieRepository.save(draft);
                })
                .doOnNext(movie -> log.info("Pelicula creada: id={} owner={}", movie.getId(),
                        movie.getOwnerUsername()));
    }

    @Override
    public Mono<Movie> findById(Long id) {
        return this.userProvider
                .getAuthenticatedUser()
                .flatMap(user -> this.movieRepository
                        .findById(id)
                        .switchIfEmpty(
                                Mono.error(new MovieNotFoundException("Movie not found: " + id)))
                        .filter(movie -> movie.getOwnerUsername().equals(user.subject()))
                        .switchIfEmpty(
                                Mono.error(new MovieNotFoundException("Movie not found: " + id))));
    }

    @Override
    public Flux<Movie> list(int limit) {
        return this.userProvider
                .getAuthenticatedUser()
                .flatMapMany(user -> this.movieRepository.findByOwner(user.subject(), Math.min(limit, 50)));
    }

    @Override
    public Mono<Movie> complete(Long id, String objectKey) {
        return this.userProvider
                .getAuthenticatedUser()
                .flatMap(user -> this.movieRepository
                        .completeIfDraft(id, user.subject(), objectKey)
                        .doOnNext(movie -> log.info(
                                "Pelicula completada: id={} owner={} object_key={}", id,
                                user.subject(), objectKey))
                        .switchIfEmpty(this.resolveConflict(id, user.subject(), objectKey)));
    }

    @Override
    public Mono<Void> delete(Long id) {
        return this.userProvider
                .getAuthenticatedUser()
                .flatMap(user -> this.movieRepository
                        .deleteById(id, user.subject())
                        .flatMap(deleted -> {
                            if (!deleted) {
                                return Mono.error(
                                        new MovieNotFoundException("Movie not found: " + id));
                            }
                            log.info("Pelicula eliminada (rollback): id={} owner={}", id,
                                    user.subject());
                            return Mono.empty();
                        }));
    }

    /**
     * Reconciliación cuando el CAS no transicionó: distingue 404 (no existe / no es del dueño),
     * no-op idempotente (ya READY con el mismo object_key) y 409 (estado no completable).
     */
    private Mono<Movie> resolveConflict(Long id, String ownerUsername, String objectKey) {
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
