ALTER TABLE media_ingestions
    ADD COLUMN IF NOT EXISTS recovery_claimed_until TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS media_ingestions_recovery_due_idx
    ON media_ingestions(phase, next_attempt_at, recovery_claimed_until);
