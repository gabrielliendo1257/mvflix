package com.gcorp.service.app.mvflix_movies.catalog.application;

import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MediaKind;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieMetadata;

public record CreateMovieCommand(MovieMetadata metadata, MediaKind kind) {}
