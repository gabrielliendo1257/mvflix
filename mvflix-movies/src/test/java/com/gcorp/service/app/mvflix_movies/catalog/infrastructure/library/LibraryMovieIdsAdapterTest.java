package com.gcorp.service.app.mvflix_movies.catalog.infrastructure.library;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieId;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAsset;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAssetId;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAssetRepository;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAssetStatus;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class LibraryMovieIdsAdapterTest {

    @Mock private MediaAssetRepository mediaAssetRepository;

    @InjectMocks private LibraryMovieIdsAdapter adapter;

    @Test
    void returnsDistinctIdentifiedMovieIdsAcrossLibraries() {
        when(this.mediaAssetRepository.findAllByLibraryId(7L))
                .thenReturn(Flux.just(asset(1L, 7L, MovieId.of(10L)), asset(2L, 7L, null)));
        when(this.mediaAssetRepository.findAllByLibraryId(8L))
                .thenReturn(Flux.just(asset(3L, 8L, MovieId.of(10L)),
                        asset(4L, 8L, MovieId.of(11L))));

        StepVerifier.create(this.adapter.findIdentifiedByLibraryIds(List.of(7L, 8L)).collectList())
                .assertNext(ids -> assertThat(ids)
                        .containsExactly(MovieId.of(10L), MovieId.of(11L)))
                .verifyComplete();
    }

    private static MediaAsset asset(long id, long libraryId, MovieId movieId) {
        Instant now = Instant.parse("2026-08-22T00:00:00Z");
        return new MediaAsset(
                MediaAssetId.of(id), libraryId, "movie-" + id + ".mkv", 100L,
                "video/x-matroska",
                movieId == null ? MediaAssetStatus.UNIDENTIFIED : MediaAssetStatus.IDENTIFIED,
                movieId, true, now, now);
    }
}
