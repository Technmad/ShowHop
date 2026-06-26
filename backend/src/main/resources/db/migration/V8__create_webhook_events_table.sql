CREATE TABLE webhook_events (
    id             UUID PRIMARY KEY,
    organizer_id   UUID        NOT NULL REFERENCES users (id),
    type           VARCHAR(30) NOT NULL,
    payload        JSONB       NOT NULL,
    occurred_at    TIMESTAMP   NOT NULL,
    fanned_out_at  TIMESTAMP,
    created_at     TIMESTAMP   NOT NULL,
    updated_at     TIMESTAMP   NOT NULL
);

-- Drives the fan-out relay's "find events not yet fanned out" scan.
CREATE INDEX ix_webhook_events_fanned_out_at ON webhook_events (fanned_out_at);
