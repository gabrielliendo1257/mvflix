CREATE TABLE users (
    id UUID PRIMARY KEY,

    username VARCHAR(50) NOT NULL,
    email VARCHAR(255) NOT NULL,

    plan VARCHAR(20) NOT NULL,

    enabled BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_users_username UNIQUE(username),
    CONSTRAINT uk_users_email UNIQUE(email)
);

CREATE INDEX idx_users_username
ON users(username);

CREATE INDEX idx_users_email
ON users(email);

CREATE INDEX idx_users_plan
ON users(plan);