-- Oppdaterer status-kolonne i utbetalingstabellen der Helved ikke sendte kvittering. (Behandling: 01997aa4-fde4-7d56-a82e-fa7ebcbf061a, Meldekort: 1909124842)
UPDATE utbetaling
SET status         = 'FERDIG',
    ekstern_status = NULL
WHERE behandling_id = '01997aa4-fde4-7d56-a82e-fa7ebcbf061a'
  AND meldekort_id = '1909124842';