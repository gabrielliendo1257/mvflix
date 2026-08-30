package com.gcorp.service.app.mvflix_movies.library.infrastructure.persistence;

import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemId;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAsset;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAssetId;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAssetRepository;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAssetStatus;

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
            catalog_item_id, created_at, updated_at, discovered_by, filename, duration,
            container, video_codec, resolution, storage_reference
            """;

    private final DatabaseClient databaseClient;

    public SpringDataMediaAssetRepository(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @Override
    public Mono<MediaAsset> save(MediaAsset asset) {
        if (asset.getId() == null) {
            var insertSpec =
                    this.databaseClient
                            .sql(
                                    """
                                    INSERT INTO media_assets
                                        (library_id, relative_path, size, mime_type, status, present, catalog_item_id, discovered_by,
                                         filename, duration, container, video_codec, resolution, storage_reference)
                                    VALUES (:library_id, :relative_path, :size, :mime_type, :status, :present, :catalog_item_id, :discovered_by,
                                            NULL, NULL, NULL, NULL, NULL, NULL)
                                    RETURNING
                                    """ + ASSET_COLUMNS)
                            .bind("library_id", asset.getLibraryId())
                            .bind("relative_path", asset.getRelativePath())
                            .bind("size", asset.getSize())
                            .bind("mime_type", asset.getMimeType())
                            .bind("status", asset.getStatus().name())
                            .bind("present", asset.getPresent())
                            .bindNull("catalog_item_id", Long.class);
            return this.bindDiscoveredBy(insertSpec, asset.getDiscoveredBy())
                    .map((row, metadata) -> this.toDomain(row))
                    .one();
        }
        var spec =
                this.databaseClient
                        .sql(
                                """
                                UPDATE media_assets
                                SET size = :size, mime_type = :mime_type, status = :status,
                                    present = :present, catalog_item_id = :catalog_item_id, updated_at = NOW()
                                WHERE id = :id
                                RETURNING
                                """ + ASSET_COLUMNS)
                        .bind("size", asset.getSize())
                        .bind("mime_type", asset.getMimeType())
                        .bind("status", asset.getStatus().name())
                        .bind("present", asset.getPresent())
                        .bind("id", asset.getId().value());
        return bindCatalogItemId(spec, asset.getCatalogItemId())
                .map((row, metadata) -> this.toDomain(row))
                .one();
    }

    private static org.springframework.r2dbc.core.DatabaseClient.GenericExecuteSpec bindCatalogItemId(
            org.springframework.r2dbc.core.DatabaseClient.GenericExecuteSpec spec,
            CatalogItemId catalogItemId) {
        if (catalogItemId == null) {
            return spec.bindNull("catalog_item_id", Long.class);
        }
        return spec.bind("catalog_item_id", catalogItemId.value());
    }

    private static org.springframework.r2dbc.core.DatabaseClient.GenericExecuteSpec bindDiscoveredBy(
            org.springframework.r2dbc.core.DatabaseClient.GenericExecuteSpec spec,
            String discoveredBy) {
        if (discoveredBy == null) {
            return spec.bindNull("discovered_by", String.class);
        }
        return spec.bind("discovered_by", discoveredBy);
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
    public Mono<MediaAsset> identifyIfUnidentified(
            MediaAssetId assetId, CatalogItemId catalogItemId) {
        return this.databaseClient
                .sql(
                        """
                        UPDATE media_assets
                         SET status = 'IDENTIFIED', catalog_item_id = :catalog_item_id, updated_at = NOW()
                        WHERE id = :asset_id
                          AND status = 'UNIDENTIFIED'
                           AND catalog_item_id IS NULL
                        RETURNING
                        """ + ASSET_COLUMNS)
                .bind("catalog_item_id", catalogItemId.value())
                .bind("asset_id", assetId.value())
                .map((row, metadata) -> this.toDomain(row))
                .one();
    }

    /**
     * Asset PREFERIDO para reproducción de una película. Política única del
     * servicio (compartida con la proyección owned del catálogo):
     * presente primero, luego el más antiguo. Así playback y catálogo nunca
     * anuncian versiones distintas para el mismo contenido.
     */
    @Override
    public Mono<MediaAsset> findByCatalogItemId(CatalogItemId catalogItemId) {
        return this.databaseClient
                .sql(
                        """
                        SELECT
                        """ + ASSET_COLUMNS
                        + """
                        FROM media_assets
                         WHERE catalog_item_id = :catalog_item_id
                        ORDER BY present DESC, id
                        LIMIT 1
                        """)
                .bind("catalog_item_id", catalogItemId.value())
                .map((row, metadata) -> this.toDomain(row))
                .one();
    }

    @Override
    public Mono<Long> unlinkByCatalogItemId(CatalogItemId catalogItemId) {
        return this.databaseClient
                .sql(
                        """
                        UPDATE media_assets
                         SET status = 'UNIDENTIFIED', catalog_item_id = NULL, updated_at = NOW()
                         WHERE catalog_item_id = :catalog_item_id
                        """)
                .bind("catalog_item_id", catalogItemId.value())
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
        Long catalogItemId = row.get("catalog_item_id", Long.class);
        Boolean present = row.get("present", Boolean.class);
        return new MediaAsset(
                MediaAssetId.of(row.get("id", Long.class)),
                row.get("library_id", Long.class),
                row.get("relative_path", String.class),
                row.get("size", Long.class),
                row.get("mime_type", String.class),
                MediaAssetStatus.valueOf(row.get("status", String.class)),
                catalogItemId == null ? null : CatalogItemId.of(catalogItemId),
                present == null || present,
                row.get("created_at", Instant.class),
                row.get("updated_at", Instant.class),
                row.get("discovered_by", String.class),
                row.get("filename", String.class), row.get("duration", Long.class),
                row.get("container", String.class), row.get("video_codec", String.class),
                row.get("resolution", String.class), row.get("storage_reference", String.class));
    }
}
