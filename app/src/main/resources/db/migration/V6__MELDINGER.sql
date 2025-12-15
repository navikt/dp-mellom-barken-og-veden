ALTER TABLE feil
    ADD COLUMN IF NOT EXISTS msg TEXT;

CREATE TABLE IF NOT EXISTS melding
(
    id                   uuid PRIMARY KEY,
    behandling_id        uuid,
    type                 text                                      NOT NULL,
    json                 jsonb                                     NOT NULL,
    tidspunkt timestamptz DEFAULT NOW()
);

CREATE OR REPLACE VIEW utbetalinglogg AS
SELECT id,
       json ->> 'sakId'        AS sak_id,
       json ->> 'behandlingId' AS behandling_id,
       json ->> 'ident'        AS ident,
       tidspunkt,
       json
FROM melding where type = 'UTBETALING_SENDT_TIL_HELVED';