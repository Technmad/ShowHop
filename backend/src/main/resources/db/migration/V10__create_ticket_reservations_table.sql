CREATE TABLE ticket_reservations (
    id                 UUID PRIMARY KEY,
    ticket_type_id     UUID         NOT NULL REFERENCES ticket_types (id),
    buyer_id           UUID         NOT NULL REFERENCES users (id),
    quantity           INTEGER      NOT NULL,
    state              VARCHAR(20)  NOT NULL,
    expires_at         TIMESTAMP    NOT NULL,
    razorpay_order_id  VARCHAR(100),
    razorpay_payment_id VARCHAR(100),
    idempotency_key    VARCHAR(255) NOT NULL,
    created_at         TIMESTAMP    NOT NULL,
    updated_at         TIMESTAMP    NOT NULL,
    CONSTRAINT uq_ticket_reservations_idempotency_key UNIQUE (idempotency_key)
);

CREATE INDEX ix_ticket_reservations_ticket_type_id ON ticket_reservations (ticket_type_id);
CREATE INDEX ix_ticket_reservations_buyer_id ON ticket_reservations (buyer_id);
-- Drives both the activeHolds availability count and the reaper's claim query.
CREATE INDEX ix_ticket_reservations_state_expires_at ON ticket_reservations (state, expires_at);
