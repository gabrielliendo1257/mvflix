package com.gcorp.service.app.mvflix_movies.application.movie;

import com.gcorp.service.app.mvflix_movies.app.security.UserProvider;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieNotFoundException;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeleteMovieUseCase {

    private final MovieRepository movieRepository;
    private final UserProvider userProvider;

    public Mono<Void> execute(Long id) {
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
}