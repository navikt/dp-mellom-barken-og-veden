CREATE TABLE IF NOT EXISTS utbetaling
(
    behandling_id    uuid PRIMARY KEY,
    basert_paa_id    uuid null references utbetaling(behandling_id),
    meldekort_id     text not NULL,
    ident            TEXT                     NOT NULL,
    status           TEXT                     NOT NULL,
    behandlet_av     TEXT                     NULL,
    opprettet        TIMESTAMP WITH TIME ZONE not null
);

CREATE TABLE IF NOT EXISTS utbetalingsdag
(
    behandling_id    uuid NOT NULL REFERENCES utbetaling (behandling_id),
    meldeperiode     text NOT NULL,
    dato             DATE NOT NULL,
    sats             int not null,
    utbetalt_beløp   int not null,

    primary key (behandling_id, dato)
);

create table status
(
    behandling_id       uuid NOT NULL REFERENCES utbetaling (behandling_id),
    status              TEXT not null ,
    opprettet           timestamp WITH TIME ZONE not null DEFAULT NOW(),

    primary key (behandling_id, status, opprettet)
);

-- tabell for kvitteringer
-- tabell for feil