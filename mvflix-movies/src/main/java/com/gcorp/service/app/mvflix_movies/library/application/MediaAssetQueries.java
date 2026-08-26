package com.gcorp.service.app.mvflix_movies.library.application;

import com.gcorp.service.app.mvflix_movies.shared.application.security.UserProvider;
import com.gcorp.service.app.mvflix_movies.library.application.port.CatalogItemAccess;
import com.gcorp.service.app.mvflix_movies.library.domain.CatalogItemId;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAsset;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAssetId;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAssetNotFoundException;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAssetRepository;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAssetStatus;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Lecturas de assets con autorización explícita:
 *
 * <ul>
 *   <li>Por biblioteca: cada quien ve sus descubrimientos; admin ve todo.
 *       Los huérfanos (previos al sello {@code discovered_by}) son solo del
 *       admin.</li>
 *   <li>Por id: identificado exige visibilidad de su película; no
 *       identificado es asunto de gestión y queda en admin. En ambos casos un
 *       no-autorizado recibe not-found sin revelar existencia.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class MediaAssetQueries {

    private final MediaAssetRepository assetRepository;
    private final CatalogItemAccess catalogItemAccess;
    private final UserProvider userProvider;

    public Flux<MediaAsset> findByLibrary(Long libraryId, MediaAssetStatus status) {
        return this.userProvider
                .getAuthenticatedUser()
                .flatMapMany(user -> {
                    Flux<MediaAsset> assets = status == null
                            ? this.assetRepository.findAllByLibraryId(libraryId)
                            : this.assetRepository.findAllByLibraryIdAndStatus(libraryId, status);
                    return user.isAdmin()
                            ? assets
                            : assets.filter(asset ->
                                    user.subject().equals(asset.getDiscoveredBy()));
                });
    }

    public Mono<MediaAsset> findById(MediaAssetId id) {
        return this.userProvider
                .getAuthenticatedUser()
                .flatMap(user -> this.assetRepository
                        .findById(id)
                        .switchIfEmpty(Mono.error(
                                new MediaAssetNotFoundException(
                                        "Media asset not found: " + id.value())))
                        .flatMap(asset -> this.authorize(user, asset)));
    }

    private Mono<MediaAsset> authorize(com.gcorp.service.app.mvflix_movies.shared.application.security.AuthenticatedUser user,
            MediaAsset asset) {
        if (user.isAdmin()) {
            return Mono.just(asset);
        }
        if (!asset.isIdentified() || asset.getCatalogItemId() == null) {
            // Gestión de ingesta: sin catálogo visible que la ampare.
            return Mono.error(notFound(asset));
        }
        return this.catalogItemAccess
                .requireVisible(asset.getCatalogItemId(), user.subject())
                .thenReturn(asset)
                .onErrorResume(error -> Mono.error(notFound(asset)));
    }

    private static MediaAssetNotFoundException notFound(MediaAsset asset) {
        return new MediaAssetNotFoundException(
                "Media asset not found: " + asset.getId().value());
    }

    public Mono<MediaAsset> findByCatalogItem(CatalogItemId catalogItemId) {
        return this.userProvider
                .getAuthenticatedUser()
                .flatMap(user -> this.catalogItemAccess
                        .requireVisible(catalogItemId, user.subject())
                        .then(Mono.defer(() -> this.assetRepository
                                .findByCatalogItemId(catalogItemId)
                                .switchIfEmpty(Mono.error(new MediaAssetNotFoundException(
                                        "No media asset for movie: " + catalogItemId.value()))))));
    }
}
