package com.gcorp.service.app.mvflix_movies.application.scan;

import com.gcorp.service.app.mvflix_movies.app.security.UserProvider;
import com.gcorp.service.app.mvflix_movies.domain.mediaasset.MediaAsset;
import com.gcorp.service.app.mvflix_movies.domain.mediaasset.MediaAssetId;
import com.gcorp.service.app.mvflix_movies.domain.mediaasset.MediaAssetNotFoundException;
import com.gcorp.service.app.mvflix_movies.domain.mediaasset.MediaAssetRepository;
import com.gcorp.service.app.mvflix_movies.domain.mediaasset.MediaAssetStatus;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieAccessDeniedException;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieId;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class MediaAssetQueries {

    private final MediaAssetRepository assetRepository;
    private final MovieRepository movieRepository;
    private final UserProvider userProvider;

    public Flux<MediaAsset> findByLibrary(Long libraryId, MediaAssetStatus status) {
        return status == null
                ? this.assetRepository.findAllByLibraryId(libraryId)
                : this.assetRepository.findAllByLibraryIdAndStatus(libraryId, status);
    }

    public Mono<MediaAsset> findById(MediaAssetId id) {
        return this.assetRepository
                .findById(id)
                .switchIfEmpty(Mono.error(
                        new MediaAssetNotFoundException("Media asset not found: " + id.value())));
    }

    public Mono<MediaAsset> findByMovie(MovieId movieId) {
        return this.userProvider
                .getAuthenticatedUser()
                .flatMap(user -> this.movieRepository
                        .findById(movieId)
                        .filter(movie -> movie.isVisibleTo(user.subject()))
                        .switchIfEmpty(Mono.error(new MovieAccessDeniedException(
                                "Movie not accessible: " + movieId.value()))))
                .flatMap(movie -> this.assetRepository
                        .findByMovieId(movieId)
                        .switchIfEmpty(Mono.error(new MediaAssetNotFoundException(
                                "No media asset for movie: " + movieId.value()))));
    }
}
