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

    private static final String ASSET_COLUMNS =
            """
            id, library_id, relative_path, size, mime_type, status, present,
            movie_id, created_at, updated_at
            """;

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
                                (library_id, relative_path, size, mime_type, status, present, movie_id)
                            VALUES (:library_id, :relative_path, :size, :mime_type, :status, :present, :movie_id)
                            RETURNING
                            """ + ASSET_COLUMNS)
                    .bind("library_id", asset.getLibraryId())
                    .bind("relative_path", asset.getRelativePath())
                    .bind("size", asset.getSize())
                    .bind("mime_type", asset.getMimeType())
                    .bind("status", asset.getStatus().name())
                    .bind("present", asset.getPresent())
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
                                    present = :present, movie_id = :movie_id, updated_at = NOW()
                                WHERE id = :id
                                RETURNING
                                """ + ASSET_COLUMNS)
                        .bind("size", asset.getSize())
                        .bind("mime_type", asset.getMimeType())
                        .bind("status", asset.getStatus().name())
                        .bind("present", asset.getPresent())
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
                        SELECT
                        """ + ASSET_COLUMNS
                        + """
                        FROM media_assets
                        WHERE id = :id
                        """)
                .bind("id", id.value())
                .map((row, metadata) -> this.toDomain(row))
                .one();
    }

    @Override
    public Mono<MediaAsset> identifyIfUnidentified(MediaAssetId assetId, MovieId movieId) {
        return this.databaseClient
                .sql(
                        """
                        UPDATE media_assets
                        SET status = 'IDENTIFIED', movie_id = :movie_id, updated_at = NOW()
                        WHERE id = :asset_id
                          AND status = 'UNIDENTIFIED'
                          AND movie_id IS NULL
                        RETURNING
                        """ + ASSET_COLUMNS)
                .bind("movie_id", movieId.value())
                .bind("asset_id", assetId.value())
                .map((row, metadata) -> this.toDomain(row))
                .one();
    }

    @Override
    public Mono<MediaAsset> findByMovieId(MovieId movieId) {
        return this.databaseClient
                .sql(
                        """
                        SELECT
                        """ + ASSET_COLUMNS
                        + """
                        FROM media_assets
                        WHERE movie_id = :movie_id
                        ORDER BY id
                        LIMIT 1
                        """)
                .bind("movie_id", movieId.value())
                .map((row, metadata) -> this.toDomain(row))
                .one();
    }

    @Override
    public Mono<Long> unlinkByMovieId(MovieId movieId) {
        return this.databaseClient
                .sql(
                        """
                        UPDATE media_assets
                        SET status = 'UNIDENTIFIED', movie_id = NULL, updated_at = NOW()
                        WHERE movie_id = :movie_id
                        """)
                .bind("movie_id", movieId.value())
                .fetch()
                .rowsUpdated()
                .map(Long::valueOf);
    }

    @Override
    public Mono<MediaAsset> findByLibraryAndPath(Long libraryId, String relativePath) {
        return this.databaseClient
                .sql(
                        """
                        SELECT
                        """ + ASSET_COLUMNS
                        + """
                        FROM media_assets
                        WHERE library_id = :library_id AND relative_path = :relative_path
                        """)
                .bind("library_id", libraryId)
                .bind("relative_path", relativePath)
                .map((row, metadata) -> this.toDomain(row))
                .one();
    }

    @Override
    public Flux<MediaAsset> findAllByLibraryId(Long libraryId) {
        return this.databaseClient
                .sql(
                        """
                        SELECT
                        """ + ASSET_COLUMNS
                        + """
                        FROM media_assets
                        WHERE library_id = :library_id
                        ORDER BY relative_path
                        """)
                .bind("library_id", libraryId)
                .map((row, metadata) -> this.toDomain(row))
                .all();
    }

    @Override
    public Flux<MediaAsset> findAllByLibraryIdAndStatus(Long libraryId, MediaAssetStatus status) {
        return this.databaseClient
                .sql(
                        """
                        SELECT
                        """ + ASSET_COLUMNS
                        + """
                        FROM media_assets
                        WHERE library_id = :library_id AND status = :status
                        ORDER BY relative_path
                        """)
                .bind("library_id", libraryId)
                .bind("status", status.name())
                .map((row, metadata) -> this.toDomain(row))
                .all();
    }

    private MediaAsset toDomain(io.r2dbc.spi.Row row) {
        Long movieId = row.get("movie_id", Long.class);
        Boolean present = row.get("present", Boolean.class);
        return new MediaAsset(
                MediaAssetId.of(row.get("id", Long.class)),
                row.get("library_id", Long.class),
                row.get("relative_path", String.class),
                row.get("size", Long.class),
                row.get("mime_type", String.class),
                MediaAssetStatus.valueOf(row.get("status", String.class)),
                movieId == null ? null : MovieId.of(movieId),
                present == null || present,
                row.get("created_at", Instant.class),
                row.get("updated_at", Instant.class));
    }
}
