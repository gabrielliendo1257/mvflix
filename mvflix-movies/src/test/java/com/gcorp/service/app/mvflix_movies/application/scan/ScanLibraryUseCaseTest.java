package com.gcorp.service.app.mvflix_movies.application.scan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gcorp.service.app.mvflix_movies.domain.mediaasset.MediaAsset;
import com.gcorp.service.app.mvflix_movies.domain.mediaasset.MediaAssetId;
import com.gcorp.service.app.mvflix_movies.domain.mediaasset.MediaAssetRepository;
import com.gcorp.service.app.mvflix_movies.domain.mediaasset.MediaAssetStatus;
import com.gcorp.service.app.mvflix_movies.domain.mediaasset.ScannedFile;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieId;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class ScanLibraryUseCaseTest {

    private static final long STORAGE_ID = 7L;

    @Mock private MediaAssetRepository assetRepository;

    @InjectMocks private ScanLibraryUseCase useCase;

    @Test
    void upsertsNewFilesAsUnidentified() {
        ScannedFile dune = new ScannedFile("Dune.mp4", 1024, "video/mp4");
        when(this.assetRepository.findByStorageAndPath(STORAGE_ID, "Dune.mp4"))
                .thenReturn(Mono.empty());
        when(this.assetRepository.save(any(MediaAsset.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(this.assetRepository.findAllByStorageId(STORAGE_ID)).thenReturn(Flux.empty());

        StepVerifier.create(this.useCase.execute(STORAGE_ID, List.of(dune)))
                .expectNextMatches(asset ->
                        asset.getStorageId() == STORAGE_ID
                                && asset.getRelativePath().equals("Dune.mp4")
                                && asset.getStatus() == MediaAssetStatus.UNIDENTIFIED
                                && !asset.isIdentified())
                .verifyComplete();
    }

    @Test
    void marksVanishedAssetsAsMissing() {
        ScannedFile present = new ScannedFile("present.mkv", 100, "video/x-matroska");
        MediaAsset vanished =
                new MediaAsset(
                        MediaAssetId.of(1L),
                        STORAGE_ID,
                        "vanished.mkv",
                        200,
                        "video/x-matroska",
                        MediaAssetStatus.IDENTIFIED,
                        MovieId.of(9L),
                        Instant.now(),
                        Instant.now());
        when(this.assetRepository.findByStorageAndPath(STORAGE_ID, "present.mkv"))
                .thenReturn(Mono.empty());
        when(this.assetRepository.save(any(MediaAsset.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(this.assetRepository.findAllByStorageId(STORAGE_ID)).thenReturn(Flux.just(vanished));

        StepVerifier.create(this.useCase.execute(STORAGE_ID, List.of(present)))
                .expectNextCount(1)
                .expectNextMatches(asset ->
                        asset.getRelativePath().equals("vanished.mkv")
                                && asset.isMissing()
                                && asset.getMovieId() != null)
                .verifyComplete();
    }

    @Test
    void refreshesStaleSizeAndMime() {
        MediaAsset stale =
                new MediaAsset(
                        MediaAssetId.of(1L),
                        STORAGE_ID,
                        "Dune.mp4",
                        100,
                        "video/mp4",
                        MediaAssetStatus.UNIDENTIFIED,
                        null,
                        Instant.now(),
                        Instant.now());
        ScannedFile fresh = new ScannedFile("Dune.mp4", 2048, "video/mp4");
        when(this.assetRepository.findByStorageAndPath(STORAGE_ID, "Dune.mp4"))
                .thenReturn(Mono.just(stale));
        when(this.assetRepository.findAllByStorageId(STORAGE_ID)).thenReturn(Flux.empty());
        when(this.assetRepository.save(any(MediaAsset.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        ArgumentCaptor<MediaAsset> saved = ArgumentCaptor.forClass(MediaAsset.class);

        StepVerifier.create(this.useCase.execute(STORAGE_ID, List.of(fresh)))
                .expectNextMatches(asset -> asset.getSize() == 2048)
                .verifyComplete();

        verify(this.assetRepository).save(saved.capture());
        assertThat(saved.getValue().getRelativePath()).isEqualTo("Dune.mp4");
    }

    @Test
    void marksRecoveredAssetsAsPresentAgain() {
        MediaAsset missing =
                new MediaAsset(
                        MediaAssetId.of(1L),
                        STORAGE_ID,
                        "Dune.mp4",
                        100,
                        "video/mp4",
                        MediaAssetStatus.MISSING,
                        MovieId.of(3L),
                        Instant.now(),
                        Instant.now());
        ScannedFile recovered = new ScannedFile("Dune.mp4", 100, "video/mp4");
        when(this.assetRepository.findByStorageAndPath(STORAGE_ID, "Dune.mp4"))
                .thenReturn(Mono.just(missing));
        when(this.assetRepository.findAllByStorageId(STORAGE_ID)).thenReturn(Flux.empty());
        when(this.assetRepository.save(any(MediaAsset.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(this.useCase.execute(STORAGE_ID, List.of(recovered)))
                .expectNextMatches(asset ->
                        !asset.isMissing()
                                && asset.getStatus() == MediaAssetStatus.IDENTIFIED)
                .verifyComplete();
    }

    @Test
    void emptyScanMarksEverythingMissingButDoesNotWriteNewRows() {
        MediaAsset orphan =
                new MediaAsset(
                        MediaAssetId.of(1L),
                        STORAGE_ID,
                        "orphan.mkv",
                        200,
                        "video/x-matroska",
                        MediaAssetStatus.UNIDENTIFIED,
                        null,
                        Instant.now(),
                        Instant.now());
        when(this.assetRepository.findAllByStorageId(STORAGE_ID)).thenReturn(Flux.just(orphan));
        when(this.assetRepository.save(any(MediaAsset.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(this.useCase.execute(STORAGE_ID, List.of()))
                .expectNextMatches(MediaAsset::isMissing)
                .verifyComplete();
    }
}
