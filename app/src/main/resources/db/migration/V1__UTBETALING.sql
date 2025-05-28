CREATE TABLE IF NOT EXISTS utbetaling
(
    behandling_id    uuid PRIMARY KEY,
    basert_paa_id    uuid                     NULL REFERENCES utbetaling (behandling_id),
    vedtakstidspunkt TIMESTAMP WITH TIME ZONE NOT NULL,
    meldekort_id     text                     NOT NULL,
    sak_id           text                     NOT NULL,
    ident            TEXT                     NOT NULL,
    status           TEXT                     NOT NULL,
    saksbehandlet_av TEXT                     NULL,
    besluttet_av     TEXT                     NULL,
    opprettet        TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS utbetalingsdag
(
    behandling_id  uuid NOT NULL REFERENCES utbetaling (behandling_id),
    meldeperiode   text NOT NULL,
    dato           DATE NOT NULL,
    sats           int  NOT NULL,
    utbetalt_beløp int  NOT NULL,

    PRIMARY KEY (behandling_id, dato)
);

CREATE TABLE status
(
    behandling_id uuid                     NOT NULL REFERENCES utbetaling (behandling_id),
    status        TEXT                     NOT NULL,
    opprettet     timestamp WITH TIME ZONE NOT NULL DEFAULT NOW(),

    PRIMARY KEY (behandling_id, status, opprettet)
);

-- tabell for kvitteringer
-- tabell for feil