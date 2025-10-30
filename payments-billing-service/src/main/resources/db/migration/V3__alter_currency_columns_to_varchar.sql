ALTER TABLE invoices
    ALTER COLUMN currency TYPE VARCHAR(3),
    ALTER COLUMN currency SET DEFAULT 'lkr';

ALTER TABLE payments
    ALTER COLUMN currency TYPE VARCHAR(3),
    ALTER COLUMN currency SET DEFAULT 'lkr';
