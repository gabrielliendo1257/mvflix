package com.gcorp.service.app.mvflix_movies.catalog.application;

import com.gcorp.service.app.mvflix_movies.shared.application.security.UserProvider;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.Movie;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieAccessDeniedException;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieId;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieRepository;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieVisibility;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

/**
 * Acceso completo de una película en UNA decisión transaccional:
 * visibilidad + compartidos se aplican juntos o no se aplican. Sustituye la
 * coreografía de dos casos de uso que podía dejar estado parcialmente
 * modificado si el segundo paso fallaba. Solo el dueño; el resto ve 403.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateMovieAccessUseCase {

    private final MovieRepository movieRepository;
    private final UserProvider userProvider;

    public Mono<Movie> execute(MovieId id, MovieVisibility visibility, List<String> sharedWith) {
        List<String> clean = sharedWith == null
                ? List.of()
                : sharedWith.stream()
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
                        .map(movie -> movie.withAccess(visibility, Set.copyOf(clean)))
                        .flatMap(this.movieRepository::updateAccess)
                        .doOnNext(updated -> log.info(
                                "Movie {} acceso -> {} compartidos={}",
                                id.value(), visibility, clean)));
    }
}
