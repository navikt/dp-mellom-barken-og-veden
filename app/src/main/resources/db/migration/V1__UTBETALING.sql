CREATE TABLE IF NOT EXISTS utbetaling
(
    behandling_id    uuid PRIMARY KEY,
    basert_paa_id    uuid null references utbetaling(behandling_id),
    meldekort_id     uuid not NULL,
    ident            TEXT                     NOT NULL,
    behandlet_av     TEXT                     NULL,
    status           TEXT                     NOT NULL,
    opprettet        TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS utbetalingsdag
(
    behandling_id    uuid NOT NULL REFERENCES utbetaling (behandling_id),
    meldeperiode     text NOT NULL,
    dato             DATE NOT NULL,
    sats             int not null,
    utbetalt_beløp   int not null
);