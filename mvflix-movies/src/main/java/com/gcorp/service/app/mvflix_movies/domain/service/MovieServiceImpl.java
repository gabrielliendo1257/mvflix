package com.gcorp.service.app.mvflix_movies.domain.service;

import com.gcorp.service.app.mvflix_movies.app.security.AuthenticatedUser;
import com.gcorp.service.app.mvflix_movies.app.security.UserProvider;
import com.gcorp.service.app.mvflix_movies.domain.exceptions.MovieNotFoundException;
import com.gcorp.service.app.mvflix_movies.domain.model.Movie;
import com.gcorp.service.app.mvflix_movies.domain.model.MovieStatus;
import com.gcorp.service.app.mvflix_movies.domain.ports.MovieRepository;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
                .flatMap(user -> {
                    Movie draft = new Movie(
                            null,
                            user.subject(),
                            command.metadata().title(),
                            MovieStatus.DRAFT,
                            null,
                            command.metadata());
                    return this.movieRepository.save(draft);
                });
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
}
