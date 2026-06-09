CREATE TABLE ticket_types (
    id              UUID PRIMARY KEY,
    event_id        UUID           NOT NULL REFERENCES events (id),
    name            VARCHAR(255)   NOT NULL,
    description     VARCHAR(1000),
    price           NUMERIC(10, 2) NOT NULL,
    total_available INTEGER        NOT NULL,
    created_at      TIMESTAMP      NOT NULL,
    updated_at      TIMESTAMP      NOT NULL
);

CREATE INDEX ix_ticket_types_event_id ON ticket_types (event_id);
