ALTER TABLE media_asset_renditions
    DROP CONSTRAINT media_asset_renditions_status,
    DROP CONSTRAINT media_asset_renditions_ready_object;

ALTER TABLE media_asset_renditions
    ADD CONSTRAINT media_asset_renditions_status CHECK
        (status IN ('REQUESTED', 'PROCESSING', 'READY', 'FAILED')),
    ADD CONSTRAINT media_asset_renditions_ready_object CHECK
        (status <> 'READY' OR storage_object_id IS NOT NULL);

CREATE UNIQUE INDEX uq_media_asset_renditions_media_profile
    ON media_asset_renditions (media_id, profile);

CREATE UNIQUE INDEX uq_media_asset_renditions_asset_profile
    ON media_asset_renditions (media_asset_id, profile);
