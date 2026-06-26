CREATE TABLE webhook_deliveries (
    id                  UUID PRIMARY KEY,
    endpoint_id         UUID        NOT NULL REFERENCES webhook_endpoints (id),
    event_id            UUID        NOT NULL REFERENCES webhook_events (id),
    state               VARCHAR(20) NOT NULL,
    attempt             INTEGER     NOT NULL DEFAULT 0,
    max_attempts        INTEGER     NOT NULL DEFAULT 8,
    is_probe            BOOLEAN     NOT NULL DEFAULT FALSE,
    next_retry_at       TIMESTAMP,
    locked_by           VARCHAR(100),
    locked_until        TIMESTAMP,
    last_response_code  INTEGER,
    last_error          VARCHAR(2000),
    created_at          TIMESTAMP   NOT NULL,
    updated_at          TIMESTAMP   NOT NULL
);

CREATE INDEX ix_webhook_deliveries_endpoint_id ON webhook_deliveries (endpoint_id);
CREATE INDEX ix_webhook_deliveries_event_id ON webhook_deliveries (event_id);
-- Drives the delivery worker's claim query: rows due now, in a claimable state.
CREATE INDEX ix_webhook_deliveries_claimable ON webhook_deliveries (state, next_retry_at);
