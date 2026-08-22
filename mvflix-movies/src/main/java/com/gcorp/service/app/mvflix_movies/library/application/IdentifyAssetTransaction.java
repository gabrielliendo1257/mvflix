package com.gcorp.service.app.mvflix_movies.library.application;

import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MediaKind;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAsset;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAssetAlreadyIdentifiedException;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAssetRepository;
import com.gcorp.service.app.mvflix_movies.library.application.port.CatalogItemCreator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

/** Frontera transaccional local para crear la película y vincular el asset mediante CAS. */
@Service
@RequiredArgsConstructor
class IdentifyAssetTransaction {

    private final CatalogItemCreator catalogItemCreator;
    private final MediaAssetRepository assetRepository;

    @Transactional(transactionManager = "connectionFactoryTransactionManager")
    Mono<IdentificationResult> execute(
            MediaAsset asset, String ownerUsername, String title, MediaKind kind) {
        return this.catalogItemCreator
                .createFromLibrary(ownerUsername, title, kind)
                .flatMap(movieId -> this.assetRepository
                        .identifyIfUnidentified(asset.getId(), movieId)
                        .map(identified -> new IdentificationResult(identified, movieId))
                        .switchIfEmpty(Mono.error(
                                new MediaAssetAlreadyIdentifiedException(
                                        "Media asset already identified: "
                                                + asset.getId().value()))));
    }
}
