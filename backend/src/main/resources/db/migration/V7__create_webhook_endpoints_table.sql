CREATE TABLE webhook_endpoints (
    id                     UUID PRIMARY KEY,
    organizer_id           UUID         NOT NULL REFERENCES users (id),
    url                    VARCHAR(2048) NOT NULL,
    secret                 VARCHAR(255) NOT NULL,
    subscribed_event_types JSONB        NOT NULL,
    status                 VARCHAR(20)  NOT NULL,
    consecutive_failures   INTEGER      NOT NULL DEFAULT 0,
    circuit_opened_at      TIMESTAMP,
    created_at             TIMESTAMP    NOT NULL,
    updated_at             TIMESTAMP    NOT NULL
);

CREATE INDEX ix_webhook_endpoints_organizer_id ON webhook_endpoints (organizer_id);
