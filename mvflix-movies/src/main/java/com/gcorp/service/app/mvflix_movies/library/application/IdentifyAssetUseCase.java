package com.gcorp.service.app.mvflix_movies.library.application;

import com.gcorp.service.app.mvflix_movies.shared.application.security.UserProvider;
import com.gcorp.service.app.mvflix_movies.library.application.port.CatalogItemEnricher;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAsset;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAssetAlreadyIdentifiedException;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAssetId;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAssetNotFoundException;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAssetRepository;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MediaKind;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieId;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Vincula un activo de biblioteca a una pelicula del catalogo en un solo paso.
 * La pelicula nace READY (el archivo ya existe); si el form trae un candidato
 * TMDB elegido (tmdb_id), la metadata se autocompleta en el mismo flujo, si no
 * queda RAW para enriquecer despues. Idempotente: un activo ya identificado se
 * devuelve tal cual.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdentifyAssetUseCase {

    private final MediaAssetRepository assetRepository;
    private final UserProvider userProvider;
    private final CatalogItemEnricher catalogItemEnricher;
    private final IdentifyAssetTransaction identifyAssetTransaction;

    public Mono<MediaAsset> execute(MediaAssetId assetId, String title, Long tmdbId, MediaKind kind) {
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
                    return this.linkToMovie(asset, title, tmdbId, kind);
                });
    }

    private Mono<MediaAsset> linkToMovie(MediaAsset asset, String title, Long tmdbId, MediaKind kind) {
        MediaKind resolvedKind = kind == null ? MediaKind.MOVIE : kind;
        return this.userProvider
                .getAuthenticatedUser()
                .flatMap(user -> this.identifyAssetTransaction.execute(
                        asset, user.subject(), title, resolvedKind))
                .flatMap(result -> this.enrichIfRequested(result.movieId(), tmdbId)
                        .thenReturn(result.asset()))
                .onErrorResume(
                        MediaAssetAlreadyIdentifiedException.class,
                        conflict -> this.assetRepository
                                .findById(asset.getId())
                                .filter(MediaAsset::isIdentified)
                                .switchIfEmpty(Mono.error(conflict)))
                .doOnNext(identified -> log.info(
                        "Asset {} identificado: path={} -> movie_id={}",
                        asset.getId(), asset.getRelativePath(),
                        identified.getMovieId().value()));
    }

    /**
     * Si el form eligio candidato TMDB, autocompleta la metadata en el mismo
     * paso; un fallo o una respuesta lenta de la fuente externa no rompe la
     * identificacion (queda RAW). El timeout acota la espera de TMDB para que
     * el flujo termine siempre y el activo quede identificado.
     */
    private static final Duration ENRICH_TIMEOUT = Duration.ofSeconds(20);

    private Mono<Void> enrichIfRequested(MovieId movieId, Long tmdbId) {
        if (tmdbId == null) {
            return Mono.empty();
        }
        return this.catalogItemEnricher
                .enrich(movieId, tmdbId)
                .timeout(this.ENRICH_TIMEOUT)
                .doOnSuccess(ignored -> log.info(
                        "Asset autocompletado con TMDB {} -> movie {} ENRICHED",
                        tmdbId, movieId.value()))
                .onErrorResume(error -> {
                    log.warn("Autocompletado TMDB {} fallido para movie {}: queda RAW: {}",
                            tmdbId, movieId.value(), error.getMessage());
                    return Mono.empty();
                });
    }
}
