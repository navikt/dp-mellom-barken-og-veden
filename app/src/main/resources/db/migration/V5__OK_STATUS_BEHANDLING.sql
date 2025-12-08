-- Behandling med id 019aca22-e56e-7535-8f64-2ae44edfa81c har vi ikke fått OK status fra Helved.
UPDATE utbetaling set status = 'FERDIG', ekstern_status = 'OK'
WHERE behandling_id = '019aca22-e56e-7535-8f64-2ae44edfa81c';