package com.gcorp.service.app.mvflix_movies.application.movie;

import com.gcorp.service.app.mvflix_movies.app.security.UserProvider;
import com.gcorp.service.app.mvflix_movies.domain.movie.Movie;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;

@Slf4j
@Service
@RequiredArgsConstructor
public class ListMoviesUseCase {

    private final MovieRepository movieRepository;
    private final UserProvider userProvider;

    public Flux<Movie> execute(int limit) {
        return this.userProvider
                .getAuthenticatedUser()
                .flatMapMany(
                        user -> this.movieRepository.findVisibleMovies(
                                user.subject(), Math.min(limit, 50)));
    }
}