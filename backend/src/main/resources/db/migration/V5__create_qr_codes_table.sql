CREATE TABLE qr_codes (
    id            UUID PRIMARY KEY,
    ticket_id     UUID        NOT NULL REFERENCES tickets (id),
    status        VARCHAR(20) NOT NULL,
    generated_at  TIMESTAMP   NOT NULL,
    created_at    TIMESTAMP   NOT NULL,
    updated_at    TIMESTAMP   NOT NULL
);

CREATE INDEX ix_qr_codes_ticket_id ON qr_codes (ticket_id);
