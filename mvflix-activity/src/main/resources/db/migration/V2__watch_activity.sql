CREATE TABLE watch_activity (
  owner_username VARCHAR(255) NOT NULL, movie_id BIGINT NOT NULL, media_id BIGINT,
  media_id_key BIGINT GENERATED ALWAYS AS (COALESCE(media_id, 0)) STORED,
  position_seconds BIGINT NOT NULL, duration_seconds BIGINT,
  completed BOOLEAN NOT NULL, sequence BIGINT NOT NULL CHECK (sequence > 0),
  last_watched_at TIMESTAMPTZ NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY (owner_username, movie_id, media_id_key)
);
CREATE INDEX watch_activity_history_idx ON watch_activity(owner_username, last_watched_at DESC);
CREATE INDEX watch_activity_continue_idx ON watch_activity(owner_username, completed, last_watched_at DESC);
