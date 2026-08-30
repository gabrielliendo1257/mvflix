-- Optional technical metadata. Existing and library assets remain nullable.
ALTER TABLE media
    ADD COLUMN IF NOT EXISTS filename VARCHAR(1024),
    ADD COLUMN IF NOT EXISTS duration BIGINT,
    ADD COLUMN IF NOT EXISTS container VARCHAR(128),
    ADD COLUMN IF NOT EXISTS video_codec VARCHAR(128),
    ADD COLUMN IF NOT EXISTS resolution VARCHAR(64),
    ADD COLUMN IF NOT EXISTS storage_reference VARCHAR(1024);

ALTER TABLE media_assets
    ADD COLUMN IF NOT EXISTS filename VARCHAR(1024),
    ADD COLUMN IF NOT EXISTS duration BIGINT,
    ADD COLUMN IF NOT EXISTS container VARCHAR(128),
    ADD COLUMN IF NOT EXISTS video_codec VARCHAR(128),
    ADD COLUMN IF NOT EXISTS resolution VARCHAR(64),
    ADD COLUMN IF NOT EXISTS storage_reference VARCHAR(1024);
