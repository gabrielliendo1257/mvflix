package com.gcorp.service.app.mvflix_movies.application.scan;

import static org.assertj.core.api.Assertions.assertThat;

import com.gcorp.service.app.mvflix_movies.library.domain.MediaAsset;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAssetRepository;
import com.gcorp.service.app.mvflix_movies.library.domain.ScannedFile;
import com.gcorp.service.app.mvflix_movies.domain.movie.MediaKind;
import com.gcorp.service.app.mvflix_movies.support.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ActiveProfiles("sandbox")
@SpringBootTest
class IdentifyAssetUseCaseIntegrationTest extends PostgresIntegrationTest {

  @Autowired private IdentifyAssetUseCase useCase;
  @Autowired private MediaAssetRepository assetRepository;
  @Autowired private DatabaseClient databaseClient;

  @BeforeEach
  void cleanDatabase() {
    this.databaseClient.sql("DELETE FROM media_assets").fetch().rowsUpdated().block();
    this.databaseClient.sql("DELETE FROM movies").fetch().rowsUpdated().block();
  }

  @Test
  void concurrentIdentificationCreatesOneMovieAndReturnsOneLink() {
    MediaAsset asset =
        this.assetRepository
            .save(
                MediaAsset.create(
                    7L, new ScannedFile("Dune.mkv", 1024L, "video/x-matroska")))
            .block();

    Mono<MediaAsset> first =
        this.useCase.execute(asset.getId(), "Dune", null, MediaKind.MOVIE);
    Mono<MediaAsset> second =
        this.useCase.execute(asset.getId(), "Dune", null, MediaKind.MOVIE);

    StepVerifier.create(Mono.zip(first, second))
        .assertNext(
            results -> {
              assertThat(results.getT1().getMovieId()).isNotNull();
              assertThat(results.getT2().getMovieId()).isEqualTo(results.getT1().getMovieId());
            })
        .verifyComplete();

    StepVerifier.create(this.countMovies()).expectNext(1L).verifyComplete();
  }

  private Mono<Long> countMovies() {
    return this.databaseClient
        .sql("SELECT COUNT(*) AS count FROM movies")
        .map((row, metadata) -> row.get("count", Long.class))
        .one();
  }
}
