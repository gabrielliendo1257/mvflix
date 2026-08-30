package com.gcorp.service.app.mvflix_movies.catalog.infrastructure.persistence;

import com.gcorp.service.app.mvflix_movies.catalog.domain.media.ManagedMediaAsset;
import com.gcorp.service.app.mvflix_movies.catalog.domain.media.MediaId;
import com.gcorp.service.app.mvflix_movies.catalog.domain.media.MediaRepository;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemId;

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
                         INSERT INTO media (catalog_item_id, object_id, object_key)
                         VALUES (:catalog_item_id, :object_id, :object_key)
                         RETURNING id, catalog_item_id, object_id, object_key, created_at
                        """)
                .bind("catalog_item_id", media.getMovieId().value())
                .bind("object_id", media.getObjectId())
                .bind("object_key", media.getObjectKey())
                .map((row, metadata) -> new ManagedMediaAsset(
                        MediaId.of(row.get("id", Long.class)),
                        CatalogItemId.of(row.get("catalog_item_id", Long.class)),
                        row.get("object_id", Long.class),
                        row.get("object_key", String.class),
                        row.get("created_at", Instant.class)))
                .one();
    }

    @Override
    public Mono<ManagedMediaAsset> findByCatalogItemId(CatalogItemId catalogItemId) {
        return this.databaseClient
                .sql(
                        """
                         SELECT id, catalog_item_id, object_id, object_key, created_at
                         FROM media
                         WHERE catalog_item_id = :catalog_item_id
                        ORDER BY id
                        LIMIT 1
                        """)
                .bind("catalog_item_id", catalogItemId.value())
                .map((row, metadata) -> new ManagedMediaAsset(
                        MediaId.of(row.get("id", Long.class)),
                        CatalogItemId.of(row.get("catalog_item_id", Long.class)),
                        row.get("object_id", Long.class),
                        row.get("object_key", String.class),
                        row.get("created_at", Instant.class)))
                .one();
    }
}
