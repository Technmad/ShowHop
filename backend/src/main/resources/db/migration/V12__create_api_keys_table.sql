CREATE TABLE api_keys (
    id           UUID PRIMARY KEY,
    organizer_id UUID         NOT NULL REFERENCES users (id),
    name         VARCHAR(255) NOT NULL,
    key_prefix   VARCHAR(16)  NOT NULL,
    hashed_key   VARCHAR(255) NOT NULL,
    last_used_at TIMESTAMP,
    revoked_at   TIMESTAMP,
    created_at   TIMESTAMP    NOT NULL,
    updated_at   TIMESTAMP    NOT NULL
);

CREATE INDEX ix_api_keys_organizer_id ON api_keys (organizer_id);
CREATE UNIQUE INDEX ux_api_keys_key_prefix ON api_keys (key_prefix);
