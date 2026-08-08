ALTER TABLE store_objects ADD COLUMN created_at TIMESTAMPTZ;

UPDATE store_objects
SET created_at = COALESCE(last_modified_at, NOW());

ALTER TABLE store_objects ALTER COLUMN created_at SET NOT NULL;