CREATE TABLE events (
    id           UUID PRIMARY KEY,
    name         VARCHAR(255) NOT NULL,
    venue        VARCHAR(255) NOT NULL,
    starts_at    TIMESTAMP    NOT NULL,
    ends_at      TIMESTAMP    NOT NULL,
    sales_start  TIMESTAMP,
    sales_end    TIMESTAMP,
    status       VARCHAR(20)  NOT NULL,
    organizer_id UUID         NOT NULL REFERENCES users (id),
    created_at   TIMESTAMP    NOT NULL,
    updated_at   TIMESTAMP    NOT NULL
);

CREATE INDEX ix_events_organizer_id ON events (organizer_id);
CREATE INDEX ix_events_status ON events (status);
