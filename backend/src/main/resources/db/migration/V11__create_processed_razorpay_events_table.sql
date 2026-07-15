-- Inbound idempotency: dedupes on Razorpay's own event id, so a redelivered
-- payment.captured (Razorpay retries on any non-2xx response) fulfills
-- exactly once. Deliberately separate from the outbound webhook_events
-- table (V8) -- that's an outbox for events *we* publish; this is a
-- dedupe ledger for events *we receive*.
CREATE TABLE processed_razorpay_events (
    razorpay_event_id VARCHAR(100) PRIMARY KEY,
    processed_at      TIMESTAMP NOT NULL
);
