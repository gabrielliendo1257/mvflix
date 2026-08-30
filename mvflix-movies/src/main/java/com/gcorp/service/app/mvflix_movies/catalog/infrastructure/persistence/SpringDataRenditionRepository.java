package com.gcorp.service.app.mvflix_movies.catalog.infrastructure.persistence;

import com.gcorp.service.app.mvflix_movies.catalog.domain.asset.MediaAssetId;
import com.gcorp.service.app.mvflix_movies.catalog.domain.asset.Rendition;
import com.gcorp.service.app.mvflix_movies.catalog.domain.asset.RenditionId;
import com.gcorp.service.app.mvflix_movies.catalog.domain.asset.RenditionOrigin;
import com.gcorp.service.app.mvflix_movies.catalog.domain.asset.RenditionRepository;
import com.gcorp.service.app.mvflix_movies.catalog.domain.asset.RenditionStatus;
import com.gcorp.service.app.mvflix_movies.catalog.domain.asset.RenditionTechnicalMetadata;
import com.gcorp.service.app.mvflix_movies.catalog.domain.asset.StorageObjectId;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;

import reactor.core.publisher.Mono;

@Repository
public class SpringDataRenditionRepository implements RenditionRepository {

    private static final String COLUMNS = "id, media_id, media_asset_id, storage_object_id, profile, status, "
            + "filename, duration, container, video_codec, resolution";

    private final DatabaseClient databaseClient;

    public SpringDataRenditionRepository(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @Override
    public Mono<Rendition> save(Rendition rendition) {
        return rendition.getId() == null ? insert(rendition) : update(rendition);
    }

    private Mono<Rendition> insert(Rendition rendition) {
        String source = rendition.getOrigin() == RenditionOrigin.MEDIA ? "media_id" : "media_asset_id";
        var spec = this.databaseClient.sql("INSERT INTO media_asset_renditions "
                + "(media_id, media_asset_id, storage_object_id, profile, status, "
                + "filename, duration, container, video_codec, resolution) "
                + "VALUES (:media_id, :media_asset_id, :storage_object_id, :profile, :status, "
                + ":filename, :duration, :container, :video_codec, :resolution) "
                + "ON CONFLICT (" + source + ", profile) DO NOTHING RETURNING " + COLUMNS);
        spec = bindSource(spec, rendition);
        spec = bindNullable(spec, "storage_object_id", rendition.getStorageObjectId() == null
                ? null : rendition.getStorageObjectId().value(), Long.class);
        spec = spec.bind("profile", rendition.getProfile()).bind("status", rendition.getStatus().name());
        spec = bindMetadata(spec, rendition);
        return spec.map((row, ignored) -> toDomain(row)).one()
                .switchIfEmpty(findBySourceAndProfile(rendition.getMediaAssetId(), rendition.getOrigin(), rendition.getProfile()));
    }

    private Mono<Rendition> update(Rendition rendition) {
        var spec = this.databaseClient.sql("UPDATE media_asset_renditions "
                + "SET storage_object_id = :storage_object_id, status = :status, "
                + "filename = :filename, duration = :duration, container = :container, "
                + "video_codec = :video_codec, resolution = :resolution, updated_at = NOW() "
                + "WHERE id = :id AND (status = :status "
                + "OR (status = 'REQUESTED' AND :status IN ('PROCESSING', 'FAILED')) "
                + "OR (status = 'PROCESSING' AND :status IN ('READY', 'FAILED'))) "
                + "RETURNING " + COLUMNS);
        spec = spec.bind("id", rendition.getId().value())
                .bind("status", rendition.getStatus().name());
        spec = bindNullable(spec, "storage_object_id", rendition.getStorageObjectId() == null
                ? null : rendition.getStorageObjectId().value(), Long.class);
        spec = bindMetadata(spec, rendition);
        return spec.map((row, ignored) -> toDomain(row)).one();
    }

    @Override
    public Mono<Rendition> findById(RenditionId id) {
        return this.databaseClient.sql("SELECT " + COLUMNS + " FROM media_asset_renditions WHERE id = :id")
                .bind("id", id.value()).map((row, ignored) -> toDomain(row)).one();
    }

    @Override
    public Mono<Rendition> findBySourceAndProfile(MediaAssetId source, RenditionOrigin origin, String profile) {
        String column = origin == RenditionOrigin.MEDIA ? "media_id" : "media_asset_id";
        return this.databaseClient.sql("SELECT " + COLUMNS + " FROM media_asset_renditions "
                        + "WHERE " + column + " = :source AND profile = :profile")
                .bind("source", source.value()).bind("profile", profile)
                .map((row, ignored) -> toDomain(row)).one();
    }

    private static org.springframework.r2dbc.core.DatabaseClient.GenericExecuteSpec bindSource(
            org.springframework.r2dbc.core.DatabaseClient.GenericExecuteSpec spec, Rendition rendition) {
        var id = rendition.getMediaAssetId().value();
        return rendition.getOrigin() == RenditionOrigin.MEDIA
                ? spec.bind("media_id", id).bindNull("media_asset_id", Long.class)
                : spec.bindNull("media_id", Long.class).bind("media_asset_id", id);
    }

    private static org.springframework.r2dbc.core.DatabaseClient.GenericExecuteSpec bindMetadata(
            org.springframework.r2dbc.core.DatabaseClient.GenericExecuteSpec spec, Rendition rendition) {
        var metadata = rendition.getTechnicalMetadata();
        spec = bindNullable(spec, "filename", metadata == null ? null : metadata.filename(), String.class);
        spec = bindNullable(spec, "duration", metadata == null ? null : metadata.duration(), Long.class);
        spec = bindNullable(spec, "container", metadata == null ? null : metadata.container(), String.class);
        spec = bindNullable(spec, "video_codec", metadata == null ? null : metadata.videoCodec(), String.class);
        return bindNullable(spec, "resolution", metadata == null ? null : metadata.resolution(), String.class);
    }

    private static <T> org.springframework.r2dbc.core.DatabaseClient.GenericExecuteSpec bindNullable(
            org.springframework.r2dbc.core.DatabaseClient.GenericExecuteSpec spec, String name, T value, Class<T> type) {
        return value == null ? spec.bindNull(name, type) : spec.bind(name, value);
    }

    private static Rendition toDomain(io.r2dbc.spi.Row row) {
        Long mediaId = row.get("media_id", Long.class);
        Long duration = row.get("duration", Long.class);
        String filename = row.get("filename", String.class);
        String container = row.get("container", String.class);
        String videoCodec = row.get("video_codec", String.class);
        String resolution = row.get("resolution", String.class);
        RenditionTechnicalMetadata metadata = filename == null && duration == null && container == null
                && videoCodec == null && resolution == null ? null
                : new RenditionTechnicalMetadata(filename, duration, container, videoCodec, resolution);
        return new Rendition(RenditionId.of(row.get("id", Long.class)),
                MediaAssetId.of(mediaId != null ? mediaId : row.get("media_asset_id", Long.class)),
                mediaId != null ? RenditionOrigin.MEDIA : RenditionOrigin.MEDIA_ASSET,
                row.get("storage_object_id", Long.class) == null ? null
                        : StorageObjectId.of(row.get("storage_object_id", Long.class)),
                row.get("profile", String.class), RenditionStatus.valueOf(row.get("status", String.class)), metadata);
    }
}
