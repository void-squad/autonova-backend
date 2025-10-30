CREATE TABLE IF NOT EXISTS invoices (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL UNIQUE,
    customer_id UUID NOT NULL,
    quote_id UUID,
    currency CHAR(3) NOT NULL DEFAULT 'LKR',
    amount_total BIGINT NOT NULL,
    status TEXT NOT NULL,
    due_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_invoices_customer ON invoices (customer_id);
CREATE INDEX IF NOT EXISTS idx_invoices_status ON invoices (status);

CREATE TABLE IF NOT EXISTS payments (
    id UUID PRIMARY KEY,
    invoice_id UUID NOT NULL REFERENCES invoices (id),
    amount BIGINT NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'LKR',
    provider TEXT NOT NULL DEFAULT 'STRIPE',
    status TEXT NOT NULL,
    stripe_payment_intent_id TEXT UNIQUE,
    failure_code TEXT,
    failure_message TEXT,
    receipt_url TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_payments_invoice ON payments (invoice_id);

CREATE TABLE IF NOT EXISTS consumed_events (
    event_id UUID PRIMARY KEY,
    type TEXT NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
