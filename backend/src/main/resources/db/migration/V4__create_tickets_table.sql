CREATE TABLE tickets (
    id             UUID PRIMARY KEY,
    ticket_type_id UUID        NOT NULL REFERENCES ticket_types (id),
    purchaser_id   UUID        NOT NULL REFERENCES users (id),
    status         VARCHAR(20) NOT NULL,
    created_at     TIMESTAMP   NOT NULL,
    updated_at     TIMESTAMP   NOT NULL
);

CREATE INDEX ix_tickets_ticket_type_id ON tickets (ticket_type_id);
CREATE INDEX ix_tickets_purchaser_id ON tickets (purchaser_id);

-- Supports the oversell-safe availability count: only PURCHASED tickets
-- consume capacity, so this index serves that filtered count directly.
CREATE INDEX ix_tickets_ticket_type_id_status ON tickets (ticket_type_id, status);
