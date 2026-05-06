ALTER TABLE utbetalingsdag
    ADD COLUMN IF NOT EXISTS dagpenge_type TEXT NOT NULL DEFAULT 'ORDINÆR';

ALTER TABLE utbetalingsdag
    ALTER COLUMN dagpenge_type DROP DEFAULT;