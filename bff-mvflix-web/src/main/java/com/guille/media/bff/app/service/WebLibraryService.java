package com.guille.media.bff.app.service;

import com.guille.media.bff.app.dto.DiscoveredFileDto;
import com.guille.media.bff.app.dto.LibraryDto;
import com.guille.media.bff.app.dto.MediaAssetDto;
import com.guille.media.bff.app.dto.MediaAssetPageDto;
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

  /** Registra una biblioteca desde un path elegido en la UI (runtime). */
  public Mono<LibraryDto> register(String rootPath) {
    log.info("register: path={}", rootPath);
    return this.storageWebClient.createLibrary(rootPath);
  }

  /** Elimina una biblioteca propia. */
  public Mono<Void> delete(Long libraryId) {
    log.info("delete: biblioteca={}", libraryId);
    return this.storageWebClient.deleteLibrary(libraryId);
  }

  /**
   * Scan de una biblioteca del operador: pregunta al storage qué archivos
   * hay y entrega la lista a movies para el upsert + marcado de MISSING.
   * El resultado reconciliado se pagina en memoria.
   */
  public Mono<MediaAssetPageDto> scan(Long libraryId, int page, int size) {
    log.info("scan: biblioteca={} page={} size={}", libraryId, page, size);
    return this.storageWebClient
        .listLibraryFiles(libraryId)
        .collectList()
        .flatMap(files -> this.reconcile(libraryId, files).collectList())
        .map(all -> paginate(all, page, size));
  }

  /** Cancela el escaneo en curso: el scanner corta en el siguiente archivo. */
  public Mono<Void> cancelScan(Long libraryId) {
    log.info("cancelScan: biblioteca={}", libraryId);
    return this.storageWebClient.cancelLibraryScan(libraryId);
  }

  private Flux<MediaAssetDto> reconcile(Long libraryId, List<DiscoveredFileDto> files) {
    return this.moviesWebClient.scanLibrary(libraryId, files);
  }

  public Mono<MediaAssetPageDto> unidentified(Long libraryId, int page, int size) {
    return this.moviesWebClient
        .listAssets(libraryId, "UNIDENTIFIED")
        .collectList()
        .map(all -> paginate(all, page, size));
  }

  private static MediaAssetPageDto paginate(List<MediaAssetDto> all, int page, int size) {
    int safePage = Math.max(page, 0);
    int safeSize = Math.max(size, 1);
    int total = all.size();
    int totalPages = (int) Math.ceil((double) total / safeSize);
    int from = Math.min(safePage * safeSize, total);
    int to = Math.min(from + safeSize, total);
    return new MediaAssetPageDto(all.subList(from, to), total, safePage, safeSize, totalPages);
  }

  /**
   * Identifica un activo en un solo paso: el titulo se deriva del filename si el
   * front no lo manda, y tmdb_id autocompleta la metadata si se eligio candidato.
   */
  public Mono<MediaAssetDto> identify(Long assetId, String title, Long tmdbId) {
    if (title != null && !title.isBlank()) {
      return this.moviesWebClient.identifyAsset(assetId, title, tmdbId);
    }
    return this.moviesWebClient
        .assetById(assetId)
        .flatMap(asset -> this.moviesWebClient.identifyAsset(
            assetId, this.titleFrom(asset.relativePath()), tmdbId));
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
