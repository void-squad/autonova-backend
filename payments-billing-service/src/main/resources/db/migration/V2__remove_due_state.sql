-- Drop usage of the legacy DUE status in favor of direct OPEN -> PAID transitions.
UPDATE invoices
SET status = 'OPEN'
WHERE status = 'DUE';

ALTER TABLE invoices
    DROP COLUMN IF EXISTS due_at;
