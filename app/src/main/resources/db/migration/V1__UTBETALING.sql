CREATE TABLE IF NOT EXISTS utbetaling
(
    utbetaling_id    uuid PRIMARY KEY,
    tilstand         TEXT                     NOT NULL,
    ident            TEXT                     NOT NULL,
    forventet_ferdig TIMESTAMP                NOT NULL,
    opprettet        TIMESTAMP WITH TIME ZONE DEFAULT NOW()
)