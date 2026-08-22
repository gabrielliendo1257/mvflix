package com.gcorp.service.app.mvflix_movies.application.movie;

import com.gcorp.service.app.mvflix_movies.app.security.UserProvider;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAssetRepository;
import com.gcorp.service.app.mvflix_movies.domain.movie.Movie;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieId;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieNotFoundException;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieRepository;

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
    private final MediaAssetRepository mediaAssetRepository;
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
                        .flatMap(movie -> this.mediaAssetRepository
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
