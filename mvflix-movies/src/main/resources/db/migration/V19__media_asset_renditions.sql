CREATE TABLE media_asset_renditions (
    id               BIGSERIAL PRIMARY KEY,
    media_id         BIGINT REFERENCES media(id) ON DELETE CASCADE,
    media_asset_id   BIGINT REFERENCES media_assets(id) ON DELETE CASCADE,
    storage_object_id BIGINT,
    profile          VARCHAR(128) NOT NULL,
    status           VARCHAR(32) NOT NULL DEFAULT 'REQUESTED',
    filename         VARCHAR(1024),
    duration         BIGINT,
    container        VARCHAR(128),
    video_codec      VARCHAR(128),
    resolution       VARCHAR(64),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT media_asset_renditions_one_source CHECK (
        (media_id IS NOT NULL AND media_asset_id IS NULL)
        OR (media_id IS NULL AND media_asset_id IS NOT NULL)
    ),
    CONSTRAINT media_asset_renditions_status CHECK (status IN ('REQUESTED', 'READY', 'FAILED')),
    CONSTRAINT media_asset_renditions_duration CHECK (duration IS NULL OR duration >= 0),
    CONSTRAINT media_asset_renditions_ready_object CHECK (status <> 'READY' OR storage_object_id IS NOT NULL)
);

CREATE INDEX idx_media_asset_renditions_media ON media_asset_renditions (media_id);
CREATE INDEX idx_media_asset_renditions_asset ON media_asset_renditions (media_asset_id);
