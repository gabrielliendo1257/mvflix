package com.gcorp.service.app.mvflix_movies.domain.service;

import com.gcorp.service.app.mvflix_movies.domain.movie.MovieMetadata;

public record CreateMovieCommand(MovieMetadata metadata) {}
