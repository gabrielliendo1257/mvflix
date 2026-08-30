-- Existing duplicates must be resolved explicitly instead of being discarded
-- during migration.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM media_asset_renditions
        WHERE media_id IS NOT NULL
        GROUP BY media_id, profile
        HAVING COUNT(*) > 1
    ) OR EXISTS (
        SELECT 1
        FROM media_asset_renditions
        WHERE media_asset_id IS NOT NULL
        GROUP BY media_asset_id, profile
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Cannot create rendition uniqueness: duplicate source/profile rows exist';
    END IF;
END
$$;

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
