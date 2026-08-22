package com.gcorp.service.app.mvflix_movies.library.application;

import com.gcorp.service.app.mvflix_movies.app.security.UserProvider;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAsset;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAssetId;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAssetNotFoundException;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAssetRepository;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAssetStatus;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieId;
import com.gcorp.service.app.mvflix_movies.library.application.port.CatalogItemAccess;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class MediaAssetQueries {

    private final MediaAssetRepository assetRepository;
    private final CatalogItemAccess catalogItemAccess;
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
                .flatMap(user -> this.catalogItemAccess
                        .requireVisible(movieId, user.subject())
                        .then(Mono.defer(() -> this.assetRepository
                                .findByMovieId(movieId)
                                .switchIfEmpty(Mono.error(new MediaAssetNotFoundException(
                                        "No media asset for movie: " + movieId.value()))))));
    }
}
