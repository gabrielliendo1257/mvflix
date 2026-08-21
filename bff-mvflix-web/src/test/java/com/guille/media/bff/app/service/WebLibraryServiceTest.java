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

    StepVerifier.create(this.service.scan(7L, 0, 20))
        .expectNextMatches(page ->
            page.total() == 1 && page.totalPages() == 1
                && page.items().size() == 1
                && "UNIDENTIFIED".equals(page.items().get(0).status()))
        .verifyComplete();
  }

  @Test
  void scanPaginatesReconciledAssets() {
    when(this.storageWebClient.listLibraryFiles(7L))
        .thenReturn(Flux.just(
            new DiscoveredFileDto("a.mp4", 1, "video/mp4"),
            new DiscoveredFileDto("b.mp4", 1, "video/mp4"),
            new DiscoveredFileDto("c.mp4", 1, "video/mp4")));
    when(this.moviesWebClient.scanLibrary(eq(7L), anyList()))
        .thenReturn(Flux.just(
            new MediaAssetDto(1L, 7L, "a.mp4", 1, "video/mp4", "UNIDENTIFIED", null),
            new MediaAssetDto(2L, 7L, "b.mp4", 1, "video/mp4", "UNIDENTIFIED", null),
            new MediaAssetDto(3L, 7L, "c.mp4", 1, "video/mp4", "UNIDENTIFIED", null)));

    StepVerifier.create(this.service.scan(7L, 1, 2))
        .expectNextMatches(page ->
            page.items().size() == 1
                && page.items().get(0).id().equals(3L)
                && page.total() == 3
                && page.page() == 1
                && page.size() == 2
                && page.totalPages() == 2)
        .verifyComplete();
  }

  @Test
  void unidentifiedPaginatesAssets() {
    when(this.moviesWebClient.listAssets(7L, "UNIDENTIFIED"))
        .thenReturn(Flux.just(
            new MediaAssetDto(1L, 7L, "a.mp4", 1, "video/mp4", "UNIDENTIFIED", null),
            new MediaAssetDto(2L, 7L, "b.mp4", 1, "video/mp4", "UNIDENTIFIED", null),
            new MediaAssetDto(3L, 7L, "c.mp4", 1, "video/mp4", "UNIDENTIFIED", null)));

    StepVerifier.create(this.service.unidentified(7L, 0, 2))
        .expectNextMatches(page ->
            page.items().size() == 2
                && page.items().get(0).id().equals(1L)
                && page.total() == 3
                && page.totalPages() == 2)
        .verifyComplete();
  }

  @Test
  void identifyUsesExplicitTitleWhenProvided() {
    when(this.moviesWebClient.identifyAsset(1L, "Dune", null, null))
        .thenReturn(Mono.just(new MediaAssetDto(
            1L, 7L, "Dune.mp4", 10, "video/mp4", "IDENTIFIED", 50L)));

    StepVerifier.create(this.service.identify(1L, "Dune", null, null))
        .expectNextMatches(asset -> "IDENTIFIED".equals(asset.status()))
        .verifyComplete();

    verify(this.moviesWebClient).identifyAsset(1L, "Dune", null, null);
  }

  @Test
  void identifyForwardsTmdbCandidateForAutocomplete() {
    when(this.moviesWebClient.identifyAsset(1L, "Dune", 123L, null))
        .thenReturn(Mono.just(new MediaAssetDto(
            1L, 7L, "Dune.mp4", 10, "video/mp4", "IDENTIFIED", 50L)));

    StepVerifier.create(this.service.identify(1L, "Dune", 123L, null))
        .expectNextMatches(asset -> "IDENTIFIED".equals(asset.status()))
        .verifyComplete();

    verify(this.moviesWebClient).identifyAsset(1L, "Dune", 123L, null);
  }

  @Test
  void identifyDerivesTitleFromFilenameWhenMissing() {
    when(this.moviesWebClient.assetById(1L))
        .thenReturn(Mono.just(new MediaAssetDto(
            1L, 7L, "Carpetas/Interstellar (2014).mkv", 10, "video/x-matroska",
            "UNIDENTIFIED", null)));
    when(this.moviesWebClient.identifyAsset(1L, "Interstellar (2014)", null, null))
        .thenReturn(Mono.just(new MediaAssetDto(
            1L, 7L, "Carpetas/Interstellar (2014).mkv", 10, "video/x-matroska",
            "IDENTIFIED", 50L)));

    StepVerifier.create(this.service.identify(1L, null, null, null))
        .expectNextMatches(asset -> "IDENTIFIED".equals(asset.status()))
        .verifyComplete();

    verify(this.moviesWebClient).identifyAsset(1L, "Interstellar (2014)", null, null);
  }

  @Test
  void librariesPassthrough() {
    when(this.storageWebClient.listLibraries())
        .thenReturn(Flux.just(new LibraryDto(1L, "LOCAL", true)));

    StepVerifier.create(this.service.libraries())
        .expectNextMatches(library -> library.id().equals(1L) && "LOCAL".equals(library.type()))
        .verifyComplete();
  }

  @Test
  void registerForwardsPathToStorage() {
    when(this.storageWebClient.createLibrary("/tmp/media/familia"))
        .thenReturn(Mono.just(new LibraryDto(9L, "LOCAL", true)));

    StepVerifier.create(this.service.register("/tmp/media/familia"))
        .expectNextMatches(library -> library.id().equals(9L))
        .verifyComplete();
  }

  @Test
  void deleteForwardsToStorage() {
    when(this.storageWebClient.deleteLibrary(9L)).thenReturn(Mono.empty());

    StepVerifier.create(this.service.delete(9L)).verifyComplete();

    org.mockito.Mockito.verify(this.storageWebClient).deleteLibrary(9L);
  }

  private static List<DiscoveredFileDto> anyList() {
    return org.mockito.ArgumentMatchers.anyList();
  }
}
