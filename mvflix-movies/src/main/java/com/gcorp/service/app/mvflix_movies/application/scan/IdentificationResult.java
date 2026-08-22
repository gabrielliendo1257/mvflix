package com.gcorp.service.app.mvflix_movies.application.scan;

import com.gcorp.service.app.mvflix_movies.domain.mediaasset.MediaAsset;
import com.gcorp.service.app.mvflix_movies.domain.movie.Movie;

record IdentificationResult(MediaAsset asset, Movie movie) {}
