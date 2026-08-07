CREATE TABLE store_objects (
    storage_id BIGSERIAL PRIMARY KEY,

    owner_username VARCHAR(255) NOT NULL,
    object_key VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,

    content_type VARCHAR(128) NOT NULL,
    content_length BIGINT NOT NULL,
    checksum VARCHAR(255),
    last_modified_at TIMESTAMPTZ,

    CONSTRAINT uk_store_objects_object_key UNIQUE(object_key)
);

CREATE INDEX idx_store_objects_owner_username
ON store_objects(owner_username);

CREATE INDEX idx_store_objects_status
ON store_objects(status);

CREATE TABLE user_storage (
    user_storage_id BIGSERIAL PRIMARY KEY,

    owner_username VARCHAR(255) NOT NULL,
    bucket_name VARCHAR(255) NOT NULL,
    storage_quota BIGINT NOT NULL,
    storage_usage BIGINT NOT NULL DEFAULT 0,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_user_storage_owner_username UNIQUE(owner_username)
);

CREATE INDEX idx_user_storage_owner_username
ON user_storage(owner_username);
