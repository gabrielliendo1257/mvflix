package com.gcorp.service.app.mvflix_movies.catalog.infrastructure.persistence;

import com.gcorp.service.app.mvflix_movies.catalog.domain.media.ManagedMediaAsset;
import com.gcorp.service.app.mvflix_movies.catalog.domain.media.MediaId;
import com.gcorp.service.app.mvflix_movies.catalog.domain.media.MediaRepository;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItemId;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;

import reactor.core.publisher.Mono;

import java.time.Instant;

@Repository
public class SpringDataMediaRepository implements MediaRepository {

    private final DatabaseClient databaseClient;

    public SpringDataMediaRepository(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @Override
    public Mono<ManagedMediaAsset> save(ManagedMediaAsset media) {
        return this.databaseClient
                .sql(
                        """
                        INSERT INTO media (movie_id, object_id, object_key)
                        VALUES (:movie_id, :object_id, :object_key)
                        RETURNING id, movie_id, object_id, object_key, created_at
                        """)
                .bind("movie_id", media.getMovieId().value())
                .bind("object_id", media.getObjectId())
                .bind("object_key", media.getObjectKey())
                .map((row, metadata) -> new ManagedMediaAsset(
                        MediaId.of(row.get("id", Long.class)),
                        CatalogItemId.of(row.get("movie_id", Long.class)),
                        row.get("object_id", Long.class),
                        row.get("object_key", String.class),
                        row.get("created_at", Instant.class)))
                .one();
    }

    @Override
    public Mono<ManagedMediaAsset> findByMovieId(CatalogItemId movieId) {
        return this.databaseClient
                .sql(
                        """
                        SELECT id, movie_id, object_id, object_key, created_at
                        FROM media
                        WHERE movie_id = :movie_id
                        ORDER BY id
                        LIMIT 1
                        """)
                .bind("movie_id", movieId.value())
                .map((row, metadata) -> new ManagedMediaAsset(
                        MediaId.of(row.get("id", Long.class)),
                        CatalogItemId.of(row.get("movie_id", Long.class)),
                        row.get("object_id", Long.class),
                        row.get("object_key", String.class),
                        row.get("created_at", Instant.class)))
                .one();
    }
}
