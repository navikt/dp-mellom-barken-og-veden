ALTER TABLE utbetaling
    ADD COLUMN IF NOT EXISTS behandlet_hendelse_type TEXT NOT NULL DEFAULT 'IKKE_MIGRERT';

ALTER TABLE utbetaling
    ALTER COLUMN behandlet_hendelse_type DROP DEFAULT;