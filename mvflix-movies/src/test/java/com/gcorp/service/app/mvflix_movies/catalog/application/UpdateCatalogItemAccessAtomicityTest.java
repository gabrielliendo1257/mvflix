package com.gcorp.service.app.mvflix_movies.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.gcorp.service.app.mvflix_movies.catalog.domain.item.MediaKind;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItem;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieMetadata;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemRepository;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemVisibility;
import com.gcorp.service.app.mvflix_movies.support.PostgresIntegrationTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.ActiveProfiles;

import reactor.test.StepVerifier;

/**
 * La promesa del caso de uso es la ATOMICIDAD: visibilidad y compartidos se
 * persisten juntos. Este test comprueba el estado FINAL en ambas tablas.
 */
@ActiveProfiles("sandbox")
@SpringBootTest
class UpdateCatalogItemAccessAtomicityTest extends PostgresIntegrationTest {

    @Autowired private UpdateCatalogItemAccessUseCase useCase;
    @Autowired private CatalogItemRepository movieRepository;
    @Autowired private DatabaseClient databaseClient;

    private long movieId;

    @BeforeEach
    void seed() {
        this.databaseClient.sql("DELETE FROM movie_shares").fetch().rowsUpdated().block();
        this.databaseClient.sql("DELETE FROM catalog_items").fetch().rowsUpdated().block();

        CatalogItem movie = this.movieRepository.save(CatalogItem.createDraft(
                "pepe", MovieMetadata.onlyTitle("Dune"), MediaKind.MOVIE)).block();
        this.movieId = movie.getId().value();
        share("maria");
        share("pedro");
        // SHARED con dos compartidos: punto de partida con residuos a limpiar.
        this.databaseClient.sql("UPDATE catalog_items SET visibility = 'SHARED' WHERE id = :id")
                .bind("id", this.movieId).then().block();
    }

    private void share(String username) {
        this.databaseClient.sql(
                "INSERT INTO movie_shares (catalog_item_id, shared_with) VALUES (:m, :u)")
                .bind("m", this.movieId).bind("u", username).then().block();
    }

    private long sharesCount() {
        Long n = this.databaseClient.sql(
                "SELECT COUNT(*) AS n FROM movie_shares WHERE catalog_item_id = :id")
                .bind("id", this.movieId)
                .map((row, meta) -> row.get("n", Long.class))
                .one().block();
        return n == null ? 0 : n;
    }

    @Test
    void sharedToPrivateCleansSharesAndVisibilityTogether() {
        StepVerifier.create(this.useCase.execute(
                        com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemId.of(this.movieId),
                        CatalogItemVisibility.PRIVATE, java.util.List.of()))
                .assertNext(updated -> {
                    assertThat(updated.getVisibility()).isEqualTo(CatalogItemVisibility.PRIVATE);
                    assertThat(updated.getSharedWith()).isEmpty();
                })
                .verifyComplete();

        assertThat(sharesCount()).isZero();
        var persisted = this.movieRepository.findById(
                com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemId.of(this.movieId)).block();
        assertThat(persisted.getVisibility()).isEqualTo(CatalogItemVisibility.PRIVATE);
    }

    @Test
    void accessChangeReplacesShareSetInOneShot() {
        StepVerifier.create(this.useCase.execute(
                        com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemId.of(this.movieId),
                        CatalogItemVisibility.SHARED, java.util.List.of("sofia")))
                .assertNext(updated ->
                        assertThat(updated.getSharedWith()).containsExactly("sofia"))
                .verifyComplete();

        var persisted = this.movieRepository.findById(
                com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemId.of(this.movieId)).block();
        assertThat(persisted.getVisibility()).isEqualTo(CatalogItemVisibility.SHARED);
        assertThat(sharesCount()).isEqualTo(1);
    }

    @Test
    void failedShareInsertRollsBackVisibilityToo() {
        // Punto de partida PRIVATE sin shares.
        this.useCase.execute(
                        com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemId.of(this.movieId),
                        CatalogItemVisibility.PRIVATE, java.util.List.of())
                .block();
        assertThat(sharesCount()).isZero();

        // username > 255 rompe el INSERT de movie_shares DESPUÉS de haber
        // cambiado la visibilidad a SHARED: la transacción debe revertir AMBOS.
        String tooLong = "x".repeat(300);
        StepVerifier.create(this.useCase.execute(
                        com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemId.of(this.movieId),
                        CatalogItemVisibility.SHARED, java.util.List.of(tooLong)))
                .expectError()
                .verify();

        // Visibilidad revertida a PRIVATE y shares sin cambios: atomicidad.
        var persisted = this.movieRepository.findById(
                com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemId.of(this.movieId)).block();
        assertThat(persisted.getVisibility()).isEqualTo(CatalogItemVisibility.PRIVATE);
        assertThat(sharesCount()).isZero();
    }
}
