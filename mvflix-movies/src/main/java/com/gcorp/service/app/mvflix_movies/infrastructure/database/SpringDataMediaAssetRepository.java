package com.gcorp.service.app.mvflix_movies.infrastructure.database;

import com.gcorp.service.app.mvflix_movies.domain.mediaasset.MediaAsset;
import com.gcorp.service.app.mvflix_movies.domain.mediaasset.MediaAssetId;
import com.gcorp.service.app.mvflix_movies.domain.mediaasset.MediaAssetRepository;
import com.gcorp.service.app.mvflix_movies.domain.mediaasset.MediaAssetStatus;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieId;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Repository
public class SpringDataMediaAssetRepository implements MediaAssetRepository {

    private final DatabaseClient databaseClient;

    public SpringDataMediaAssetRepository(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @Override
    public Mono<MediaAsset> save(MediaAsset asset) {
        if (asset.getId() == null) {
            return this.databaseClient
                    .sql(
                            """
                            INSERT INTO media_assets
                                (storage_id, relative_path, size, mime_type, status, movie_id)
                            VALUES (:storage_id, :relative_path, :size, :mime_type, :status, :movie_id)
                            RETURNING id, storage_id, relative_path, size, mime_type, status,
                                      movie_id, created_at, updated_at
                            """)
                    .bind("storage_id", asset.getStorageId())
                    .bind("relative_path", asset.getRelativePath())
                    .bind("size", asset.getSize())
                    .bind("mime_type", asset.getMimeType())
                    .bind("status", asset.getStatus().name())
                    .bindNull("movie_id", Long.class)
                    .map((row, metadata) -> this.toDomain(row))
                    .one();
        }
        var spec =
                this.databaseClient
                        .sql(
                                """
                                UPDATE media_assets
                                SET size = :size, mime_type = :mime_type, status = :status,
                                    movie_id = :movie_id, updated_at = NOW()
                                WHERE id = :id
                                RETURNING id, storage_id, relative_path, size, mime_type, status,
                                          movie_id, created_at, updated_at
                                """)
                        .bind("size", asset.getSize())
                        .bind("mime_type", asset.getMimeType())
                        .bind("status", asset.getStatus().name())
                        .bind("id", asset.getId().value());
        return bindMovieId(spec, asset.getMovieId())
                .map((row, metadata) -> this.toDomain(row))
                .one();
    }

    private static org.springframework.r2dbc.core.DatabaseClient.GenericExecuteSpec bindMovieId(
            org.springframework.r2dbc.core.DatabaseClient.GenericExecuteSpec spec, MovieId movieId) {
        if (movieId == null) {
            return spec.bindNull("movie_id", Long.class);
        }
        return spec.bind("movie_id", movieId.value());
    }

    @Override
    public Mono<MediaAsset> findById(MediaAssetId id) {
        return this.databaseClient
                .sql(
                        """
                        SELECT id, storage_id, relative_path, size, mime_type, status,
                               movie_id, created_at, updated_at
                        FROM media_assets
                        WHERE id = :id
                        """)
                .bind("id", id.value())
                .map((row, metadata) -> this.toDomain(row))
                .one();
    }

    @Override
    public Mono<MediaAsset> findByStorageAndPath(Long storageId, String relativePath) {
        return this.databaseClient
                .sql(
                        """
                        SELECT id, storage_id, relative_path, size, mime_type, status,
                               movie_id, created_at, updated_at
                        FROM media_assets
                        WHERE storage_id = :storage_id AND relative_path = :relative_path
                        """)
                .bind("storage_id", storageId)
                .bind("relative_path", relativePath)
                .map((row, metadata) -> this.toDomain(row))
                .one();
    }

    @Override
    public Flux<MediaAsset> findAllByStorageId(Long storageId) {
        return this.databaseClient
                .sql(
                        """
                        SELECT id, storage_id, relative_path, size, mime_type, status,
                               movie_id, created_at, updated_at
                        FROM media_assets
                        WHERE storage_id = :storage_id
                        ORDER BY relative_path
                        """)
                .bind("storage_id", storageId)
                .map((row, metadata) -> this.toDomain(row))
                .all();
    }

    @Override
    public Flux<MediaAsset> findAllByStorageIdAndStatus(Long storageId, MediaAssetStatus status) {
        return this.databaseClient
                .sql(
                        """
                        SELECT id, storage_id, relative_path, size, mime_type, status,
                               movie_id, created_at, updated_at
                        FROM media_assets
                        WHERE storage_id = :storage_id AND status = :status
                        ORDER BY relative_path
                        """)
                .bind("storage_id", storageId)
                .bind("status", status.name())
                .map((row, metadata) -> this.toDomain(row))
                .all();
    }

    private MediaAsset toDomain(io.r2dbc.spi.Row row) {
        Long movieId = row.get("movie_id", Long.class);
        return new MediaAsset(
                MediaAssetId.of(row.get("id", Long.class)),
                row.get("storage_id", Long.class),
                row.get("relative_path", String.class),
                row.get("size", Long.class),
                row.get("mime_type", String.class),
                MediaAssetStatus.valueOf(row.get("status", String.class)),
                movieId == null ? null : MovieId.of(movieId),
                row.get("created_at", Instant.class),
                row.get("updated_at", Instant.class));
    }
}
