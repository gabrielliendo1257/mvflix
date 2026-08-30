package com.gcorp.service.app.mvflix_movies.catalog.infrastructure.persistence;

import com.gcorp.service.app.mvflix_movies.catalog.domain.media.ManagedMediaAsset;
import com.gcorp.service.app.mvflix_movies.catalog.domain.media.MediaId;
import com.gcorp.service.app.mvflix_movies.catalog.domain.media.MediaRepository;
import com.gcorp.service.app.mvflix_movies.catalog.domain.media.StorageObjectId;
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
        var spec = this.databaseClient
                .sql(
                        """
                         INSERT INTO media (catalog_item_id, object_id, object_key, filename,
                                            duration, container, video_codec, resolution, storage_reference)
                         VALUES (:catalog_item_id, :object_id, :object_key, :filename,
                                 :duration, :container, :video_codec, :resolution, :storage_reference)
                         RETURNING id, catalog_item_id, object_id, object_key, filename,
                                   duration, container, video_codec, resolution, storage_reference, created_at
                        """)
                .bind("catalog_item_id", media.getMovieId().value())
                .bind("object_id", media.getStorageObjectId().value())
                .bind("object_key", media.getObjectKey());
        spec = bindNullable(spec, "filename", media.getFilename(), String.class);
        spec = bindNullable(spec, "duration", media.getDuration(), Long.class);
        spec = bindNullable(spec, "container", media.getContainer(), String.class);
        spec = bindNullable(spec, "video_codec", media.getVideoCodec(), String.class);
        spec = bindNullable(spec, "resolution", media.getResolution(), String.class);
        return bindNullable(spec, "storage_reference", media.getStorageReference(), String.class)
                .map((row, metadata) -> new ManagedMediaAsset(
                        MediaId.of(row.get("id", Long.class)),
                        CatalogItemId.of(row.get("catalog_item_id", Long.class)),
                        StorageObjectId.of(row.get("object_id", Long.class)),
                        row.get("object_key", String.class),
                        row.get("filename", String.class), row.get("duration", Long.class),
                        row.get("container", String.class), row.get("video_codec", String.class),
                        row.get("resolution", String.class), row.get("storage_reference", String.class),
                        row.get("created_at", Instant.class)))
                .one();
    }

    private static <T> org.springframework.r2dbc.core.DatabaseClient.GenericExecuteSpec bindNullable(
            org.springframework.r2dbc.core.DatabaseClient.GenericExecuteSpec spec,
            String name, T value, Class<T> type) {
        return value == null ? spec.bindNull(name, type) : spec.bind(name, value);
    }

    @Override
    public Mono<ManagedMediaAsset> findByCatalogItemId(CatalogItemId catalogItemId) {
        return this.databaseClient
                .sql(
                        """
                         SELECT id, catalog_item_id, object_id, object_key, filename,
                                duration, container, video_codec, resolution, storage_reference, created_at
                         FROM media
                         WHERE catalog_item_id = :catalog_item_id
                        ORDER BY id
                        LIMIT 1
                        """)
                .bind("catalog_item_id", catalogItemId.value())
                .map((row, metadata) -> new ManagedMediaAsset(
                        MediaId.of(row.get("id", Long.class)),
                        CatalogItemId.of(row.get("catalog_item_id", Long.class)),
                        StorageObjectId.of(row.get("object_id", Long.class)),
                        row.get("object_key", String.class),
                        row.get("filename", String.class), row.get("duration", Long.class),
                        row.get("container", String.class), row.get("video_codec", String.class),
                        row.get("resolution", String.class), row.get("storage_reference", String.class),
                        row.get("created_at", Instant.class)))
                .one();
    }
}
