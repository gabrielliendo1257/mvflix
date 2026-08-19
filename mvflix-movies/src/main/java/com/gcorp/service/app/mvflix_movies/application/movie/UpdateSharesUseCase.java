package com.gcorp.service.app.mvflix_movies.application.movie;

import com.gcorp.service.app.mvflix_movies.app.security.UserProvider;
import com.gcorp.service.app.mvflix_movies.domain.movie.Movie;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieAccessDeniedException;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieId;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

/**
 * Reemplaza la lista de usuarios con quienes se comparte una pelicula
 * (visibilidad SHARED). Solo el dueño (lo decide {@link Movie#isOwnedBy(String)});
 * el resto ve 403.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateSharesUseCase {

    private final MovieRepository movieRepository;
    private final UserProvider userProvider;

    public Mono<Movie> execute(MovieId id, List<String> usernames) {
        List<String> clean = usernames == null
                ? List.of()
                : usernames.stream()
                        .filter(name -> name != null && !name.isBlank())
                        .distinct()
                        .toList();
        return this.userProvider
                .getAuthenticatedUser()
                .flatMap(user -> this.movieRepository
                        .findById(id)
                        .switchIfEmpty(Mono.error(new MovieAccessDeniedException(
                                "Movie not accessible: " + id.value())))
                        .filter(movie -> movie.isOwnedBy(user.subject()))
                        .switchIfEmpty(Mono.error(new MovieAccessDeniedException(
                                "Movie not owned: " + id.value())))
                        .flatMap(movie -> this.movieRepository
                                .replaceShares(id, user.subject(), clean)
                                .map(updated -> updated.withSharedWith(
                                        Set.copyOf(clean))))
                        .doOnNext(updated -> log.info(
                                "Movie {} compartida con {}",
                                id.value(), clean)));
    }
}