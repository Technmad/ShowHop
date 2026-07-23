CREATE TABLE audit_log_entries (
    id            UUID PRIMARY KEY,
    actor_user_id UUID,
    organizer_id  UUID         NOT NULL REFERENCES users (id),
    action        VARCHAR(100) NOT NULL,
    entity_type   VARCHAR(100) NOT NULL,
    entity_id     VARCHAR(255) NOT NULL,
    metadata      JSONB,
    occurred_at   TIMESTAMP    NOT NULL
);

CREATE INDEX ix_audit_log_entries_organizer_id ON audit_log_entries (organizer_id);
