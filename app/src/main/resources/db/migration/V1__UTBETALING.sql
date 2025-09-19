CREATE TABLE IF NOT EXISTS utbetaling
(
    behandling_id        uuid PRIMARY KEY,
    basert_paa_id        uuid                                      NULL,
    vedtakstidspunkt     TIMESTAMP WITH TIME ZONE                  NOT NULL,
    meldekort_id         text                                      NOT NULL,
    sak_id               uuid                                      NOT NULL,
    ident                TEXT                                      NOT NULL,
    status               TEXT                                      NOT NULL,
    ekstern_status       TEXT                                      NULL,
    saksbehandlet_av     TEXT                                      NULL,
    besluttet_av         TEXT                                      NULL,
    opprettet            TIMESTAMP WITH TIME ZONE                  NOT NULL,
    sist_endret_tilstand TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW() NOT NULL
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

CREATE TABLE ekstern_status
(
    behandling_id uuid                     NOT NULL REFERENCES utbetaling (behandling_id),
    status        TEXT                     NOT NULL,
    opprettet     timestamp WITH TIME ZONE NOT NULL DEFAULT NOW(),

    PRIMARY KEY (behandling_id, status, opprettet)
);

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