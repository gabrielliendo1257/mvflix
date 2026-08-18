package com.gcorp.service.app.mvflix_movies.application.scan;

import com.gcorp.service.app.mvflix_movies.app.security.UserProvider;
import com.gcorp.service.app.mvflix_movies.domain.mediaasset.MediaAsset;
import com.gcorp.service.app.mvflix_movies.domain.mediaasset.MediaAssetId;
import com.gcorp.service.app.mvflix_movies.domain.mediaasset.MediaAssetNotFoundException;
import com.gcorp.service.app.mvflix_movies.domain.mediaasset.MediaAssetRepository;
import com.gcorp.service.app.mvflix_movies.domain.movie.EnrichmentStatus;
import com.gcorp.service.app.mvflix_movies.domain.movie.Movie;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieMetadata;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieRepository;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Vincula un activo de biblioteca a una pelicula del catalogo. La pelicula
 * nace READY (el archivo ya existe) y RAW; el usuario la enriquece despues
 * con el flujo TMDB existente. Idempotente: un activo ya identificado se
 * devuelve tal cual.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdentifyAssetUseCase {

    private final MediaAssetRepository assetRepository;
    private final MovieRepository movieRepository;
    private final UserProvider userProvider;

    public Mono<MediaAsset> execute(MediaAssetId assetId, String title) {
        return this.assetRepository
                .findById(assetId)
                .switchIfEmpty(Mono.defer(() -> Mono.error(
                        new MediaAssetNotFoundException("Media asset not found: "
                                + assetId.value()))))
                .flatMap(asset -> {
                    if (asset.isIdentified()) {
                        log.info("Asset {} ya identificado: no-op", assetId.value());
                        return Mono.just(asset);
                    }
                    return this.linkToMovie(asset, title);
                });
    }

    private Mono<MediaAsset> linkToMovie(MediaAsset asset, String title) {
        return this.userProvider
                .getAuthenticatedUser()
                .flatMap(user -> this.movieRepository.save(new Movie(
                        null,
                        user.subject(),
                        title,
                        MovieStatus.READY,
                        EnrichmentStatus.RAW,
                        null,
                        new MovieMetadata(
                                title, null, null, List.of(), null, null, null,
                                List.of(), null, null, null, null, null,
                                List.of(), null))))
                .flatMap(movie -> this.assetRepository.save(asset.identify(movie.getId())))
                .doOnNext(identified -> log.info(
                        "Asset {} identificado: path={} -> movie_id={}",
                        asset.getId(), asset.getRelativePath(),
                        identified.getMovieId().value()));
    }
}
