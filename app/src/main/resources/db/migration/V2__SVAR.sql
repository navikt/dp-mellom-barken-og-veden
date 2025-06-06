CREATE TABLE IF NOT EXISTS detaljer_svar
(
    behandling_id uuid NOT NULL REFERENCES utbetaling (behandling_id),
    fom           DATE NOT NULL,
    tom           DATE NOT NULL,
    beløp         int  NOT NULL,
    sats          int  NULL,
    klassekode    text NOT NULL,

    PRIMARY KEY (behandling_id, fom, tom)
);

CREATE TABLE feil
(
    behandling_id uuid NOT NULL REFERENCES utbetaling (behandling_id),
    status        int  NOT NULL,
    doc           text NOT NULL
);