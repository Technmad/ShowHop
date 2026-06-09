CREATE TABLE ticket_validations (
    id                UUID PRIMARY KEY,
    ticket_id         UUID        NOT NULL REFERENCES tickets (id),
    validated_by_id   UUID        NOT NULL REFERENCES users (id),
    status            VARCHAR(20) NOT NULL,
    method            VARCHAR(20) NOT NULL,
    validated_at      TIMESTAMP   NOT NULL,
    created_at        TIMESTAMP   NOT NULL,
    updated_at        TIMESTAMP   NOT NULL
);

CREATE INDEX ix_ticket_validations_ticket_id ON ticket_validations (ticket_id);
