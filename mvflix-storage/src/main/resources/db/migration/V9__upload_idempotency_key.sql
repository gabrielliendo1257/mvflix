ALTER TABLE store_objects ADD COLUMN idempotency_key VARCHAR(255);

CREATE UNIQUE INDEX uk_store_objects_owner_idempotency
ON store_objects(owner_username, idempotency_key)
WHERE idempotency_key IS NOT NULL;
