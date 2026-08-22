package com.gcorp.service.app.mvflix_movies.library.application;

import com.gcorp.service.app.mvflix_movies.library.domain.MediaAsset;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieId;

record IdentificationResult(MediaAsset asset, MovieId movieId) {}
