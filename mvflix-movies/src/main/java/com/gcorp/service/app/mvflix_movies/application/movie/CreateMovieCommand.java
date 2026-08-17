package com.gcorp.service.app.mvflix_movies.application.movie;

import com.gcorp.service.app.mvflix_movies.domain.movie.MovieMetadata;

public record CreateMovieCommand(MovieMetadata metadata) {}
