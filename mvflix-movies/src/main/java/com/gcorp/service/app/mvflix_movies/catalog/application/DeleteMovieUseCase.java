package com.gcorp.service.app.mvflix_movies.catalog.application;

import com.gcorp.service.app.mvflix_movies.app.security.UserProvider;
import com.gcorp.service.app.mvflix_movies.catalog.application.port.LibraryAssetLinks;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.Movie;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieId;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieNotFoundException;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeleteMovieUseCase {

    private final MovieRepository movieRepository;
    private final LibraryAssetLinks libraryAssetLinks;
    private final UserProvider userProvider;

    @Transactional(transactionManager = "connectionFactoryTransactionManager")
    public Mono<Void> execute(MovieId id) {
        return this.userProvider
                .getAuthenticatedUser()
                .flatMap(user -> this.movieRepository
                        .findById(id)
                        .switchIfEmpty(Mono.error(new MovieNotFoundException(
                                "Movie not found: " + id)))
                        .filter(movie -> movie.isOwnedBy(user.subject()))
                        .switchIfEmpty(Mono.error(new MovieNotFoundException(
                                "Movie not found: " + id)))
                        .flatMap(movie -> this.libraryAssetLinks
                                .unlinkByMovieId(id)
                                .then(this.movieRepository.deleteById(id))
                                .flatMap(deleted -> {
                                    if (!deleted) {
                                        return Mono.error(
                                                new MovieNotFoundException(
                                                        "Movie not found: " + id));
                                    }
                                    log.info("Pelicula eliminada (rollback): id={} owner={}",
                                            id.value(), user.subject());
                                    return Mono.empty();
                                })));
    }
}
