package com.gcorp.service.app.mvflix_movies.application.scan;

import com.gcorp.service.app.mvflix_movies.domain.mediaasset.MediaAsset;
import com.gcorp.service.app.mvflix_movies.domain.mediaasset.MediaAssetRepository;
import com.gcorp.service.app.mvflix_movies.domain.mediaasset.ScannedFile;

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
 * marca MISSING los activos que ya no estan. No identifica nada: eso es
 * responsabilidad de {@code IdentifyAssetUseCase}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScanLibraryUseCase {

    private final MediaAssetRepository assetRepository;

    public Flux<MediaAsset> execute(Long storageId, List<ScannedFile> discovered) {
        Set<String> presentPaths = new HashSet<>();
        Flux<MediaAsset> upserts =
                Flux.fromIterable(discovered)
                        .doOnNext(file -> presentPaths.add(file.relativePath()))
                        .concatMap(file -> this.upsert(storageId, file))
                        .doOnNext(asset -> log.info(
                                "Asset presente: storage={} path={} status={}",
                                storageId, asset.getRelativePath(), asset.getStatus()));

        Flux<MediaAsset> missings =
                this.assetRepository
                        .findAllByStorageId(storageId)
                        .filter(asset -> !asset.isMissing())
                        .filter(asset -> !presentPaths.contains(asset.getRelativePath()))
                        .concatMap(asset -> this.assetRepository.save(asset.markMissing()))
                        .doOnNext(asset -> log.warn(
                                "Asset desaparecido: storage={} path={}",
                                storageId, asset.getRelativePath()));

        return upserts.concatWith(missings);
    }

    private Mono<MediaAsset> upsert(Long storageId, ScannedFile file) {
        return this.assetRepository
                .findByStorageAndPath(storageId, file.relativePath())
                .map(asset -> asset.markPresent().refresh(file.size(), file.mimeType()))
                .switchIfEmpty(Mono.defer(
                        () -> Mono.just(MediaAsset.create(storageId, file))))
                .flatMap(this.assetRepository::save);
    }
}
