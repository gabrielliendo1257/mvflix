package com.gcorp.service.app.mvflix_movies.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.gcorp.service.app.mvflix_movies.library.domain.MediaAsset;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemId;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAssetRepository;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAssetStatus;
import com.gcorp.service.app.mvflix_movies.library.domain.ScannedFile;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.MediaKind;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItem;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieMetadata;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemRepository;
import com.gcorp.service.app.mvflix_movies.support.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.ActiveProfiles;
import reactor.test.StepVerifier;

@ActiveProfiles("sandbox")
@SpringBootTest
class DeleteCatalogItemUseCaseIntegrationTest extends PostgresIntegrationTest {

  @Autowired private DeleteCatalogItemUseCase useCase;
  @Autowired private CatalogItemRepository movieRepository;
  @Autowired private MediaAssetRepository mediaAssetRepository;
  @Autowired private DatabaseClient databaseClient;

  @BeforeEach
  void cleanDatabase() {
    this.databaseClient.sql("DELETE FROM media_assets").fetch().rowsUpdated().block();
    this.databaseClient.sql("DELETE FROM catalog_items").fetch().rowsUpdated().block();
  }

  @Test
  void deletingMovieLeavesItsLibraryAssetUnidentified() {
    CatalogItem movie =
        this.movieRepository
            .save(
                CatalogItem.fromLibraryAsset(
                    "pepe", MovieMetadata.onlyTitle("Dune"), MediaKind.MOVIE))
            .block();
    MediaAsset discovered =
        this.mediaAssetRepository
            .save(
                MediaAsset.create(
 7L, new ScannedFile("Dune.mkv", 1024L, "video/x-matroska"), null))
            .block();
    MediaAsset asset =
        this.mediaAssetRepository
            .save(discovered.identify(CatalogItemId.of(movie.getId().value())))
            .block();

     StepVerifier.create(this.useCase.execute(movie.getId()))
         .expectNext(new DeletionOutcome.Completed())
         .verifyComplete();

    StepVerifier.create(this.movieRepository.findById(movie.getId())).verifyComplete();
    StepVerifier.create(this.mediaAssetRepository.findById(asset.getId()))
        .assertNext(
            persisted -> {
              assertThat(persisted.getStatus()).isEqualTo(MediaAssetStatus.UNIDENTIFIED);
              assertThat(persisted.getCatalogItemId()).isNull();
              assertThat(persisted.isPresent()).isTrue();
            })
        .verifyComplete();
  }
}
