package com.guille.media.bff.app.service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guille.media.bff.app.dto.DiscoveredFileDto;
import com.guille.media.bff.app.dto.LibraryDto;
import com.guille.media.bff.app.dto.MediaAssetDto;
import com.guille.media.bff.app.ports.MoviesWebClient;
import com.guille.media.bff.app.ports.StorageWebClient;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class WebLibraryServiceTest {

  @Mock private StorageWebClient storageWebClient;
  @Mock private MoviesWebClient moviesWebClient;

  @InjectMocks private WebLibraryService service;

  @Test
  void scanReconcilesStorageFilesIntoMovies() {
    when(this.storageWebClient.listLibraryFiles(7L))
        .thenReturn(Flux.just(
            new DiscoveredFileDto("Interstellar/Interstellar.mkv", 100, "video/x-matroska")));
    when(this.moviesWebClient.scanLibrary(eq(7L), anyList()))
        .thenReturn(Flux.just(new MediaAssetDto(
            1L, 7L, "Interstellar/Interstellar.mkv", 100, "video/x-matroska",
            "UNIDENTIFIED", null)));

    StepVerifier.create(this.service.scan(7L))
        .expectNextMatches(asset -> asset.id().equals(1L) && "UNIDENTIFIED".equals(asset.status()))
        .verifyComplete();
  }

  @Test
  void identifyUsesExplicitTitleWhenProvided() {
    when(this.moviesWebClient.identifyAsset(1L, "Dune"))
        .thenReturn(Mono.just(new MediaAssetDto(
            1L, 7L, "Dune.mp4", 10, "video/mp4", "IDENTIFIED", 50L)));

    StepVerifier.create(this.service.identify(1L, "Dune"))
        .expectNextMatches(asset -> "IDENTIFIED".equals(asset.status()))
        .verifyComplete();

    verify(this.moviesWebClient).identifyAsset(1L, "Dune");
  }

  @Test
  void identifyDerivesTitleFromFilenameWhenMissing() {
    when(this.moviesWebClient.assetById(1L))
        .thenReturn(Mono.just(new MediaAssetDto(
            1L, 7L, "Carpetas/Interstellar (2014).mkv", 10, "video/x-matroska",
            "UNIDENTIFIED", null)));
    when(this.moviesWebClient.identifyAsset(1L, "Interstellar (2014)"))
        .thenReturn(Mono.just(new MediaAssetDto(
            1L, 7L, "Carpetas/Interstellar (2014).mkv", 10, "video/x-matroska",
            "IDENTIFIED", 50L)));

    StepVerifier.create(this.service.identify(1L, null))
        .expectNextMatches(asset -> "IDENTIFIED".equals(asset.status()))
        .verifyComplete();

    verify(this.moviesWebClient).identifyAsset(1L, "Interstellar (2014)");
  }

  @Test
  void librariesPassthrough() {
    when(this.storageWebClient.listLibraries())
        .thenReturn(Flux.just(new LibraryDto(1L, "LOCAL", true)));

    StepVerifier.create(this.service.libraries())
        .expectNextMatches(library -> library.id().equals(1L) && "LOCAL".equals(library.type()))
        .verifyComplete();
  }

  private static List<DiscoveredFileDto> anyList() {
    return org.mockito.ArgumentMatchers.anyList();
  }
}
