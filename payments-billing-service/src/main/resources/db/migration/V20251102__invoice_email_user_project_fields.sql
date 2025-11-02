DROP INDEX IF EXISTS idx_invoices_customer;

ALTER TABLE invoices
    ADD COLUMN customer_email TEXT NOT NULL DEFAULT 'unknown@autonova.invalid',
    ADD COLUMN customer_user_id BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN project_name TEXT,
    ADD COLUMN project_description TEXT;

ALTER TABLE invoices
    DROP COLUMN customer_id;

ALTER TABLE invoices
    ALTER COLUMN customer_email DROP DEFAULT,
    ALTER COLUMN customer_user_id DROP DEFAULT;

CREATE INDEX IF NOT EXISTS idx_invoices_customer_email ON invoices (customer_email);
CREATE INDEX IF NOT EXISTS idx_invoices_customer_user_id ON invoices (customer_user_id);
