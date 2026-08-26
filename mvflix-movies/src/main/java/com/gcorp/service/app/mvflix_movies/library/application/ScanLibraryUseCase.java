package com.gcorp.service.app.mvflix_movies.library.application;

import com.gcorp.service.app.mvflix_movies.library.domain.MediaAsset;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAssetRepository;
import com.gcorp.service.app.mvflix_movies.library.domain.ScannedFile;
import com.gcorp.service.app.mvflix_movies.shared.application.security.UserProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Concilia el contenido de una biblioteca del operador con el catalogo:
 * hace upsert de los archivos descubiertos (idempotente por storage+path) y
 * marca como ausentes (present=false) los activos que ya no estan. No
 * identifica nada: eso es responsabilidad de {@code IdentifyAssetUseCase}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScanLibraryUseCase {

    private final MediaAssetRepository assetRepository;
    private final UserProvider userProvider;

    /**
     * El scan corre bajo la identidad de quien lo pide: cada asset nuevo queda
     * sellado con su {@code discoveredBy} para la autorización de gestión.
     */
    public Flux<MediaAsset> execute(Long libraryId, List<ScannedFile> discovered) {
        return this.userProvider
                .getAuthenticatedUser()
                .flatMapMany(user -> this.reconcile(libraryId, discovered, user.subject()));
    }

    private Flux<MediaAsset> reconcile(Long libraryId, List<ScannedFile> discovered,
            String requestedBy) {
        Set<String> presentPaths = new HashSet<>();
        Flux<MediaAsset> upserts =
                Flux.fromIterable(discovered)
                        .doOnNext(file -> presentPaths.add(file.relativePath()))
                        .concatMap(file -> this.upsert(libraryId, file, requestedBy))
                        .doOnNext(asset -> log.info(
                                "Asset presente: storage={} path={} status={} by={}",
                                libraryId, asset.getRelativePath(), asset.getStatus(),
                                asset.getDiscoveredBy()));

        Flux<MediaAsset> missings =
                this.assetRepository
                        .findAllByLibraryId(libraryId)
                        .filter(asset -> !asset.isMissing())
                        .filter(asset -> !presentPaths.contains(asset.getRelativePath()))
                        .concatMap(asset -> this.assetRepository.save(asset.markMissing()))
                        .doOnNext(asset -> log.warn(
                                "Asset desaparecido: storage={} path={}",
                                libraryId, asset.getRelativePath()));

        return upserts.concatWith(missings);
    }

    private Mono<MediaAsset> upsert(Long libraryId, ScannedFile file, String requestedBy) {
        return this.assetRepository
                .findByLibraryAndPath(libraryId, file.relativePath())
                .map(asset -> asset.markPresent().refresh(file.size(), file.mimeType()))
                .switchIfEmpty(Mono.defer(
                        () -> Mono.just(MediaAsset.create(libraryId, file, requestedBy))))
                .flatMap(this.assetRepository::save);
    }
}
