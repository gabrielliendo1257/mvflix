package com.guille.media.bff.app.service;

import com.guille.media.bff.app.dto.DiscoveredFileDto;
import com.guille.media.bff.app.dto.LibraryDto;
import com.guille.media.bff.app.dto.MediaAssetDto;
import com.guille.media.bff.app.ports.MoviesWebClient;
import com.guille.media.bff.app.ports.StorageWebClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Orquestador del media server (flujo de biblioteca): el storage descubre
 * archivos, el BFF los reconcilia en movies y los activos sin película se
 * ofrecen al front para identificar.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebLibraryService {

  private final StorageWebClient storageWebClient;
  private final MoviesWebClient moviesWebClient;

  public Flux<LibraryDto> libraries() {
    return this.storageWebClient.listLibraries();
  }

  /**
   * Scan de una biblioteca del operador: pregunta al storage qué archivos
   * hay y entrega la lista a movies para el upsert + marcado de MISSING.
   */
  public Flux<MediaAssetDto> scan(Long libraryId) {
    log.info("scan: biblioteca={}", libraryId);
    return this.storageWebClient
        .listLibraryFiles(libraryId)
        .collectList()
        .flatMapMany(files -> this.reconcile(libraryId, files));
  }

  private Flux<MediaAssetDto> reconcile(Long libraryId, List<DiscoveredFileDto> files) {
    log.info("scan: biblioteca={} archivos descubiertos={}", libraryId, files.size());
    return this.moviesWebClient.scanLibrary(libraryId, files);
  }

  public Flux<MediaAssetDto> unidentified(Long libraryId) {
    return this.moviesWebClient.listAssets(libraryId, "UNIDENTIFIED");
  }

  /** Identifica un activo; el título se deriva del filename si el front no lo manda. */
  public Mono<MediaAssetDto> identify(Long assetId, String title) {
    if (title != null && !title.isBlank()) {
      return this.moviesWebClient.identifyAsset(assetId, title);
    }
    return this.moviesWebClient
        .assetById(assetId)
        .flatMap(asset -> this.moviesWebClient.identifyAsset(
            assetId, this.titleFrom(asset.relativePath())));
  }

  /** "Carpetas/Interstellar (2014).mkv" -> "Interstellar" (último segmento sin extensión). */
  private String titleFrom(String relativePath) {
    String fileName = relativePath == null ? "" : relativePath;
    int slash = fileName.lastIndexOf('/');
    if (slash >= 0) {
      fileName = fileName.substring(slash + 1);
    }
    int dot = fileName.lastIndexOf('.');
    if (dot > 0) {
      fileName = fileName.substring(0, dot);
    }
    return fileName.isBlank() ? "Sin título" : fileName;
  }
}
