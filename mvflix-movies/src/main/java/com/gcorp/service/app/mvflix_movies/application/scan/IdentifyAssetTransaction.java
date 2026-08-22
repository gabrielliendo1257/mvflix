package com.gcorp.service.app.mvflix_movies.application.scan;

import com.gcorp.service.app.mvflix_movies.domain.mediaasset.MediaAsset;
import com.gcorp.service.app.mvflix_movies.domain.mediaasset.MediaAssetAlreadyIdentifiedException;
import com.gcorp.service.app.mvflix_movies.domain.mediaasset.MediaAssetRepository;
import com.gcorp.service.app.mvflix_movies.domain.movie.Movie;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

/** Frontera transaccional local para crear la película y vincular el asset mediante CAS. */
@Service
@RequiredArgsConstructor
class IdentifyAssetTransaction {

    private final MovieRepository movieRepository;
    private final MediaAssetRepository assetRepository;

    @Transactional(transactionManager = "connectionFactoryTransactionManager")
    Mono<IdentificationResult> execute(MediaAsset asset, Movie movie) {
        return this.movieRepository
                .save(movie)
                .flatMap(saved -> this.assetRepository
                        .identifyIfUnidentified(asset.getId(), saved.getId())
                        .map(identified -> new IdentificationResult(identified, saved))
                        .switchIfEmpty(Mono.error(
                                new MediaAssetAlreadyIdentifiedException(
                                        "Media asset already identified: "
                                                + asset.getId().value()))));
    }
}
