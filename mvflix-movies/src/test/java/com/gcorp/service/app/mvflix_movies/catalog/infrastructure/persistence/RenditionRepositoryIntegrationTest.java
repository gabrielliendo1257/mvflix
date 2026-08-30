package com.gcorp.service.app.mvflix_movies.catalog.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.gcorp.service.app.mvflix_movies.catalog.domain.asset.MediaAssetId;
import com.gcorp.service.app.mvflix_movies.catalog.domain.asset.Rendition;
import com.gcorp.service.app.mvflix_movies.catalog.domain.asset.RenditionOrigin;
import com.gcorp.service.app.mvflix_movies.catalog.domain.asset.RenditionStatus;
import com.gcorp.service.app.mvflix_movies.catalog.domain.asset.RenditionTechnicalMetadata;
import com.gcorp.service.app.mvflix_movies.catalog.domain.asset.StorageObjectId;
import com.gcorp.service.app.mvflix_movies.support.PostgresIntegrationTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("sandbox")
@SpringBootTest
class RenditionRepositoryIntegrationTest extends PostgresIntegrationTest {

    @Autowired private SpringDataRenditionRepository repository;
    @Autowired private DatabaseClient databaseClient;

    private long sourceId;

    @BeforeEach
    void cleanDatabase() {
        this.databaseClient.sql("DELETE FROM media_asset_renditions").fetch().rowsUpdated().block();
        this.sourceId = this.databaseClient.sql("""
                INSERT INTO media_assets (library_id, relative_path, size, mime_type, status)
                VALUES (1, :path, 1, 'video/mp4', 'IDENTIFIED') RETURNING id
                """).bind("path", "rendition-test-" + System.nanoTime() + ".mp4")
                .map((row, ignored) -> row.get("id", Long.class)).one().block();
    }

    @Test
    void persistsIdempotentlyAndProtectsLifecycleAndUniqueSourceProfile() {
        var requested = Rendition.requested(MediaAssetId.of(this.sourceId), RenditionOrigin.MEDIA_ASSET, "1080p");
        var inserted = this.repository.save(requested).block();

        assertThat(inserted.getStatus()).isEqualTo(RenditionStatus.REQUESTED);
        assertThat(inserted.getTechnicalMetadata()).isNull();
        var duplicate = this.repository.save(requested).block();
        assertThat(duplicate.getId()).isEqualTo(inserted.getId());

        var processing = this.repository.save(inserted.processing()).block();
        var ready = this.repository.save(processing.ready(StorageObjectId.of(999L), null)).block();
        assertThat(ready.getStatus()).isEqualTo(RenditionStatus.READY);

        var illegalFailed = new Rendition(ready.getId(), ready.getMediaAssetId(), ready.getOrigin(),
                ready.getStorageObjectId(), ready.getProfile(), RenditionStatus.FAILED, null);
        assertThat(this.repository.save(illegalFailed).block()).isNull();
        assertThat(this.repository.findById(ready.getId()).block().getStatus()).isEqualTo(RenditionStatus.READY);

        var secondProfile = Rendition.requested(MediaAssetId.of(this.sourceId), RenditionOrigin.MEDIA_ASSET, "720p");
        assertThat(this.repository.save(secondProfile).block().getId()).isNotEqualTo(inserted.getId());
    }

    @Test
    void completionIsIdempotentAndCannotReplaceReadyResult() {
        var requested = Rendition.requested(MediaAssetId.of(this.sourceId), RenditionOrigin.MEDIA_ASSET, "1080p");
        var processing = this.repository.save(requested).block();
        processing = this.repository.save(processing.processing()).block();
        var metadata = new RenditionTechnicalMetadata("movie.mp4", 120L, "mp4", "h264", "1920x1080");

        var first = this.repository.save(processing.ready(StorageObjectId.of(100L), metadata)).block();
        var identical = this.repository.save(processing.ready(StorageObjectId.of(100L), metadata)).block();
        assertThat(identical.getId()).isEqualTo(first.getId());
        assertThat(identical.getStorageObjectId()).isEqualTo(StorageObjectId.of(100L));

        var differentMetadata = new RenditionTechnicalMetadata("movie.mp4", 121L, "mp4", "h264", "1920x1080");
        assertThat(this.repository.save(processing.ready(StorageObjectId.of(100L), differentMetadata)).block())
                .isNull();
        var different = this.repository.save(processing.ready(StorageObjectId.of(200L), metadata)).block();
        assertThat(different).isNull();
        assertThat(this.repository.findById(first.getId()).block().getStorageObjectId())
                .isEqualTo(StorageObjectId.of(100L));
    }
}
