-- Behandling med id 019b1eee-018d-7739-8fba-04a482f74097 har vi ikke fått OK status fra Helved.
UPDATE utbetaling set status = 'FERDIG', ekstern_status = 'OK'
WHERE behandling_id = '019b1eee-018d-7739-8fba-04a482f74097';