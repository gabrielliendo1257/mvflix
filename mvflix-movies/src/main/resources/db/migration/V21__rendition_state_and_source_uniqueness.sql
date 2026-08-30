-- V19 did not enforce source/profile uniqueness. Keep the oldest row before
-- adding the unique indexes so existing databases can be upgraded safely.
WITH duplicate_renditions AS (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY media_id, profile ORDER BY id) AS row_number
    FROM media_asset_renditions
    WHERE media_id IS NOT NULL
    UNION ALL
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY media_asset_id, profile ORDER BY id) AS row_number
    FROM media_asset_renditions
    WHERE media_asset_id IS NOT NULL
)
DELETE FROM media_asset_renditions rendition
USING duplicate_renditions duplicate
WHERE rendition.id = duplicate.id
  AND duplicate.row_number > 1;

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
